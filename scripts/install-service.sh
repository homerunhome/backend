#!/usr/bin/env bash
set -euo pipefail

# Run on EC2 as the same non-root account used by SSH_USER.
if [[ "$(id -u)" == 0 ]]; then
  echo 'Run as the SSH login user, without sudo; this script calls sudo when needed.' >&2
  exit 1
fi
app_user="$(id -un)"
app_root="$HOME/homerunhome"
[[ "$app_user" =~ ^[a-z_][a-z0-9_-]*$ ]]
[[ "$HOME" =~ ^/[a-zA-Z0-9_/-]+$ ]]
for command in java curl flock sha256sum systemctl; do
  command -v "$command" >/dev/null
done
java_bin="$(command -v java)"
if ! "$java_bin" -version 2>&1 | grep -Eq 'version "21([.\"]|$)'; then
  echo 'Install Java 21 and select it as the default java before continuing.' >&2
  exit 1
fi

umask 077
mkdir -p "$app_root/releases"
if [[ ! -f "$app_root/app.env" ]]; then
  echo "Create $app_root/app.env directly on EC2 first. See docs/deployment.md." >&2
  exit 1
fi
chmod 600 "$app_root/app.env"

unit_file="$(mktemp)"
trap 'rm -f "$unit_file"' EXIT
cat > "$unit_file" <<EOF
[Unit]
Description=HomerunHome Backend
Wants=network-online.target
After=network-online.target mysqld.service mysql.service
StartLimitIntervalSec=0

[Service]
User=$app_user
WorkingDirectory=$app_root
EnvironmentFile=$app_root/app.env
ExecStart=$java_bin -Xms128m -Xmx384m -jar $app_root/current/app.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143
TimeoutStopSec=30
UMask=0077
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF
sudo install -m 644 "$unit_file" /etc/systemd/system/homerunhome.service
sudo systemctl daemon-reload
sudo systemctl enable homerunhome.service
sudo -n systemctl cat homerunhome.service >/dev/null
echo 'Service installed and enabled. The first successful CD run starts the application.'
