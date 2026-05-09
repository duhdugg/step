set -euxo pipefail

DEBIAN_PROOT_DIR="$PREFIX/var/lib/proot-distro/installed-rootfs/debian"
STEP_DIR="$DEBIAN_PROOT_DIR/opt/step"

function _step_attempt_homes_backup {
  if test -d "$STEP_DIR"; then
    tar -caf ~/step-homes-backup-$(date +%F).tar -C "$STEP_DIR" homes
  fi
}

function _step_main_backup_homes {
  _step_attempt_homes_backup
  echo "backup finished!"
}

_step_main_backup_homes
