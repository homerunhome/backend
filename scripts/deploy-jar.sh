#!/usr/bin/env bash
set -Eeuo pipefail

release_id="${1:?Usage: deploy-jar.sh RELEASE_ID}"
[[ "$release_id" =~ ^[a-f0-9]{40}-[0-9]+-[0-9]+$ ]]
app_root="${HOMERUNHOME_APP_ROOT:-$HOME/homerunhome}"
release_dir="$app_root/releases/$release_id"
service=homerunhome.service

test -f "$app_root/app.env"
test -s "$release_dir/app.jar"
exec 9>"$app_root/deploy.lock"
flock -w 300 9
(cd "$release_dir" && sha256sum --check --status app.jar.sha256)
sudo -n systemctl cat "$service" >/dev/null

previous=""
if [[ -L "$app_root/current" ]]; then
  previous="$(readlink "$app_root/current")"
elif [[ -e "$app_root/current" ]]; then
  echo 'current must be a symlink, not a file or directory.' >&2
  exit 1
fi
if [[ "$previous" == "$release_dir" ]]; then
  echo 'This release is already current; use a new workflow run attempt.' >&2
  exit 1
fi

switch_release() {
  ln -s "$1" "$app_root/current.next" || return 1
  mv -Tf "$app_root/current.next" "$app_root/current"
}

wait_for_app() {
  local attempt
  for attempt in {1..40}; do
    if sudo -n systemctl is-active --quiet "$service" && \
      curl --fail --silent --max-time 3 --output /dev/null \
        http://127.0.0.1:8080/api/games; then
      return 0
    fi
    sleep 3
  done
  return 1
}

rollback() {
  trap - ERR INT TERM
  set +e
  rm -f "$app_root/current.next"
  if [[ -n "$previous" ]]; then
    echo 'Deployment failed; restoring the previous JAR.' >&2
    if switch_release "$previous" && sudo -n systemctl restart "$service" && wait_for_app; then
      echo 'Previous JAR is responding again.' >&2
    else
      echo 'Rollback failed. Check systemctl status and journalctl on EC2.' >&2
    fi
  else
    sudo -n systemctl stop "$service"
    rm -f "$app_root/current"
    echo 'First deployment failed; service stopped. Check the database and app.env.' >&2
  fi
  exit 1
}

trap rollback ERR INT TERM
switch_release "$release_dir"
sudo -n systemctl restart "$service"
wait_for_app
trap - ERR INT TERM
echo "Deployment succeeded: $release_id"
