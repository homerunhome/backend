"""Exercise release switching and recovery without touching a real service or DB."""
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "deploy-jar.sh"
RELEASE_ID = "a" * 40 + "-123-1"


class DeployJarTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.home = Path(self.temp.name)
        self.root = self.home / "homerunhome"
        self.release = self.root / "releases" / RELEASE_ID
        self.release.mkdir(parents=True)
        (self.root / "app.env").write_text("DB_USERNAME=test\n")
        (self.release / "app.jar").write_bytes(b"new test jar")
        digest = hashlib.sha256(b"new test jar").hexdigest()
        (self.release / "app.jar.sha256").write_text(f"{digest}  app.jar\n")
        self.bin = self.home / "bin"
        self.bin.mkdir()
        self.stub("sudo", '''#!/usr/bin/env bash
set -eu
echo "$*" >> "$HOME/commands"
case "$*" in
  *" restart "*)
    if [[ "${FAIL_RESTART:-0}" == 1 && ! -e "$HOME/restart-failed" ]]; then
      touch "$HOME/restart-failed"
      exit 1
    fi
    ;;
esac
''')
        self.stub("curl", '''#!/usr/bin/env bash
if [[ "${FAIL_HEALTH:-0}" == 1 ]]; then
  [[ "$(readlink "$HOME/homerunhome/current")" == "$HOME/homerunhome/releases/previous" ]]
fi
''')
        self.stub("sleep", "#!/usr/bin/env bash\nexit 0\n")
        self.env = dict(os.environ, HOME=str(self.home),
                        PATH=f"{self.bin}:{os.environ['PATH']}")

    def stub(self, name, content):
        path = self.bin / name
        path.write_text(content)
        path.chmod(0o755)

    def previous_release(self):
        previous = self.root / "releases" / "previous"
        previous.mkdir()
        (previous / "app.jar").write_bytes(b"previous test jar")
        (self.root / "current").symlink_to(previous)
        return previous

    def run_deploy(self, **settings):
        return subprocess.run(["bash", str(SCRIPT), RELEASE_ID],
                              env=dict(self.env, **settings),
                              capture_output=True, text=True, timeout=10)

    def test_success_switches_current_and_retains_previous(self):
        previous = self.previous_release()
        result = self.run_deploy()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.root / "current").resolve(), self.release)
        self.assertEqual((previous / "app.jar").read_bytes(), b"previous test jar")

    def test_unhealthy_release_restores_previous(self):
        previous = self.previous_release()
        result = self.run_deploy(FAIL_HEALTH="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.root / "current").resolve(), previous)
        self.assertIn("Previous JAR is responding again", result.stderr)

    def test_restart_failure_restores_previous(self):
        previous = self.previous_release()
        result = self.run_deploy(FAIL_RESTART="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.root / "current").resolve(), previous)
        self.assertIn("Previous JAR is responding again", result.stderr)

    def test_first_deploy_failure_stops_service(self):
        result = self.run_deploy(FAIL_HEALTH="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / "current").is_symlink())
        self.assertIn("systemctl stop homerunhome.service", (self.home / "commands").read_text())

    def test_corrupt_jar_does_not_restart_or_replace_current(self):
        previous = self.previous_release()
        (self.release / "app.jar").write_bytes(b"corrupt transfer")
        result = self.run_deploy()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.root / "current").resolve(), previous)
        self.assertFalse((self.home / "commands").exists())

    def test_missing_environment_does_not_touch_current(self):
        previous = self.previous_release()
        (self.root / "app.env").unlink()
        result = self.run_deploy()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.root / "current").resolve(), previous)
        self.assertFalse((self.home / "commands").exists())


if __name__ == "__main__":
    unittest.main()
