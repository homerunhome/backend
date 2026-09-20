#!/usr/bin/env bash
set -Eeuo pipefail

# Linux tests: isolate release files and replace service/network commands with stubs.
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
release_id=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-123-1
mkdir -p "$test_dir/bin"

cat > "$test_dir/bin/sudo" <<'STUB'
#!/usr/bin/env bash
set -eu
echo "$*" >> "$TEST_ROOT/commands"
case "$*" in
  *" restart "*)
    if [[ "${FAIL_RESTART:-0}" == 1 && ! -e "$TEST_ROOT/restart-failed" ]]; then
      touch "$TEST_ROOT/restart-failed"
      exit 1
    fi
    ;;
esac
STUB
cat > "$test_dir/bin/curl" <<'STUB'
#!/usr/bin/env bash
if [[ "${FAIL_HEALTH:-0}" == 1 ]]; then
  [[ "$(readlink "$HOMERUNHOME_APP_ROOT/current")" == "$HOMERUNHOME_APP_ROOT/releases/previous" ]]
fi
STUB
cat > "$test_dir/bin/sleep" <<'STUB'
#!/usr/bin/env bash
exit 0
STUB
chmod +x "$test_dir/bin/"*

run_deploy() {
  if env PATH="$test_dir/bin:$PATH" \
    TEST_ROOT="$case_dir" HOMERUNHOME_APP_ROOT="$app_root" \
    FAIL_HEALTH="$fail_health" FAIL_RESTART="$fail_restart" \
    timeout 10 bash "$script_dir/deploy-jar.sh" "$release_id" > "$case_dir/output" 2>&1; then
    deploy_status=0
  else
    deploy_status=$?
  fi
  # A hung test must fail even when the case expects a deployment failure.
  [[ "$deploy_status" != 124 && "$deploy_status" != 137 ]]
}

run_case() (
  case_name="$1"
  case_dir="$test_dir/$case_name"
  app_root="$case_dir/app"
  release_dir="$app_root/releases/$release_id"
  previous="$app_root/releases/previous"
  fail_health=0
  fail_restart=0
  trap 'echo "FAIL: $case_name" >&2; cat "$case_dir/output" >&2 2>/dev/null || true' ERR
  mkdir -p "$release_dir"
  printf 'DB_USERNAME=test\n' > "$app_root/app.env"
  printf 'new test jar' > "$release_dir/app.jar"
  (cd "$release_dir" && sha256sum app.jar > app.jar.sha256)

  if [[ "$case_name" != first_deploy_failure ]]; then
    mkdir -p "$previous"
    printf 'previous test jar' > "$previous/app.jar"
    ln -s "$previous" "$app_root/current"
  fi

  case "$case_name" in
    success)
      run_deploy
      [[ "$deploy_status" == 0 ]]
      [[ "$(readlink "$app_root/current")" == "$release_dir" ]]
      [[ "$(cat "$previous/app.jar")" == 'previous test jar' ]]
      ;;
    unhealthy_release)
      fail_health=1
      run_deploy
      [[ "$deploy_status" == 1 ]]
      [[ "$(readlink "$app_root/current")" == "$previous" ]]
      grep -q 'Previous JAR is responding again' "$case_dir/output"
      ;;
    restart_failure)
      fail_restart=1
      run_deploy
      [[ "$deploy_status" == 1 ]]
      [[ "$(readlink "$app_root/current")" == "$previous" ]]
      grep -q 'Previous JAR is responding again' "$case_dir/output"
      ;;
    first_deploy_failure)
      fail_health=1
      run_deploy
      [[ "$deploy_status" == 1 ]]
      [[ ! -L "$app_root/current" && ! -e "$app_root/current" ]]
      grep -q 'systemctl stop homerunhome.service' "$case_dir/commands"
      ;;
    corrupt_jar)
      printf 'corrupt transfer' > "$release_dir/app.jar"
      run_deploy
      [[ "$deploy_status" == 1 ]]
      [[ "$(readlink "$app_root/current")" == "$previous" ]]
      [[ ! -e "$case_dir/commands" ]]
      ;;
    missing_environment)
      rm "$app_root/app.env"
      run_deploy
      [[ "$deploy_status" == 1 ]]
      [[ "$(readlink "$app_root/current")" == "$previous" ]]
      [[ ! -e "$case_dir/commands" ]]
      ;;
  esac
  echo "PASS: $case_name"
)

for case_name in success unhealthy_release restart_failure first_deploy_failure corrupt_jar missing_environment; do
  run_case "$case_name"
done
echo 'All 6 deployment tests passed.'
