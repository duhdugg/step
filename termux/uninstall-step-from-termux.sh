set -euxo pipefail

PROOT_DIR="$PREFIX/var/lib/proot-distro/installed-rootfs"
DEBIAN_PROOT_DIR="$PROOT_DIR/debian"
STEP_DIR="$DEBIAN_PROOT_DIR/opt/step"
STEP_BIN="/data/data/com.termux/files/usr/bin/step"


function _step_delete_step_debian_proot_if_present {
  if test -d "$DEBIAN_PROOT_DIR"; then
    if test -d "$STEP_DIR"; then
      true
    else
      echo "non-step debian proot detected"
      false
    fi
    if test -n "$(ls $PROOT_DIR | grep -v debian)"; then
      echo "non-debian proot-distro installations detected"
      false
    fi
    rm -rf "$DEBIAN_PROOT_DIR"
  fi
}

function _step_delete_step_command_if_present {
  if test -f "$STEP_BIN"; then
    rm "$STEP_BIN"
  fi
}

function _step_uninstall_proot_distro_if_unused {
  if test -d "$PROOT_DIR"; then
    if test -z "$(ls $PROOT_DIR)"; then
      _step_uninstall_proot_distro
    fi
  else
    _step_uninstall_proot_distro
  fi
}

function _step_uninstall_proot_distro {
  pkg uninstall -y proot-distro
}

function _step_main_uninstall {
  _step_delete_step_debian_proot_if_present
  _step_uninstall_proot_distro_if_unused
  _step_delete_step_command_if_present
  echo "step uninstallation complete"
}

_step_main_uninstall
