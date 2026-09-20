#!/usr/bin/env bash
set -euo pipefail

for name in SERVER_IP SSH_USER SSH_PRIVATE_KEY SSH_KNOWN_HOSTS GH_TOKEN; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing configuration: $name. See docs/deployment.md." >&2
    exit 1
  fi
done
[[ "$SERVER_IP" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*$ ]]
[[ "$SSH_USER" =~ ^[a-z_][a-z0-9_-]*$ ]]
release_id="${GITHUB_SHA}-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
[[ "$release_id" =~ ^[a-f0-9]{40}-[0-9]+-[0-9]+$ ]]
test -s build/libs/app.jar

# Reject an old workflow rerun after main has advanced.
head_sha="$(curl --fail --silent --show-error --max-time 20 \
  -H "Authorization: Bearer $GH_TOKEN" \
  "https://api.github.com/repos/$GITHUB_REPOSITORY/git/ref/heads/main" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["object"]["sha"])')"
if [[ "$head_sha" != "$GITHUB_SHA" ]]; then
  echo 'This commit is no longer main HEAD. Run the latest main workflow.' >&2
  exit 1
fi

umask 077
ssh_dir="$(mktemp -d)"
trap 'rm -rf "$ssh_dir"' EXIT
printf '%s\n' "$SSH_PRIVATE_KEY" > "$ssh_dir/key"
printf '%s\n' "$SSH_KNOWN_HOSTS" > "$ssh_dir/known_hosts"
unset SSH_PRIVATE_KEY SSH_KNOWN_HOSTS GH_TOKEN
ssh-keygen -y -P '' -f "$ssh_dir/key" >/dev/null
ssh-keygen -F "$SERVER_IP" -f "$ssh_dir/known_hosts" >/dev/null

options=(-i "$ssh_dir/key" -o BatchMode=yes -o IdentitiesOnly=yes
  -o StrictHostKeyChecking=yes -o "UserKnownHostsFile=$ssh_dir/known_hosts"
  -o ConnectTimeout=15 -o ServerAliveInterval=15 -o ServerAliveCountMax=3)
remote="$SSH_USER@$SERVER_IP"
release_dir="homerunhome/releases/$release_id"
ssh "${options[@]}" "$remote" "umask 077; mkdir -p $release_dir"
(cd build/libs && sha256sum app.jar) > "$ssh_dir/app.jar.sha256"
scp "${options[@]}" build/libs/app.jar "$ssh_dir/app.jar.sha256" \
  scripts/deploy-jar.sh "$remote:$release_dir/"
ssh "${options[@]}" "$remote" "bash $release_dir/deploy-jar.sh $release_id"
