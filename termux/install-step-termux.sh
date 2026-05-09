set -euxo pipefail

URL="https://github.com/duhdugg/step/releases/download/termux-0.0.1/step-debian-arm64.tar.gz"
TARBAL_CHECKSUM="18efb5770292129da51901bf8b8a77215e04f6cd0a0d17db2f6ed0105576a311"
DEBIAN_PROOT_DIR="$PREFIX/var/lib/proot-distro/installed-rootfs/debian"
STEP_DIR="$DEBIAN_PROOT_DIR/opt/step"

export APT_LISTCHANGES_FRONTEND=none
export DEBIAN_FRONTEND=noninteractive

function _step_check_for_existing_non_step_debian_proot {
  if test -d "$DEBIAN_PROOT_DIR"; then
    if test -d "$STEP_DIR"; then
      true
    else
      echo "Non-step debian proot already exists. A manual installation is recommended."
      exit 1
    fi
  fi
}

function _step_install_dependencies {
  apt update -y
  apt install -y proot-distro
}

function _step_attempt_homes_backup {
  if test -d "$STEP_DIR"; then
    tar -cf ~/step-homes-backup.tar -C "$STEP_DIR" homes
  fi
}

function _step_attempt_homes_restore {
  if test -f ~/step-homes-backup.tar; then
    mkdir -p "$STEP_DIR"
    tar -xf ~/step-homes-backup.tar -C "$STEP_DIR"
    rm ~/step-homes-backup.tar
  fi
}

function _step_dl_tarball {
  curl -L -o step-debian-arm64.tar.gz "$URL"
  sha256sum step-debian-arm64.tar.gz | grep "$TARBAL_CHECKSUM"
}

function _step_destroy_proot_distro {
  DISTRO_DIR="$PREFIX/var/lib/proot-distro/installed-rootfs/debian"
  if test -d "$DISTRO_DIR"; then
    rm -rf "$DISTRO_DIR"
  fi
}

function _step_extract_tarball {
  mkdir -p $PREFIX/var/lib/proot-distro/installed-rootfs/debian
  tar -xf step-debian-arm64.tar.gz \
    -C $PREFIX/var/lib/proot-distro/installed-rootfs/debian \
    --exclude="*.lock" --no-same-owner --no-same-permissions
  rm step-debian-arm64.tar.gz
}

function _step_apply_resolv_conf {
  cat /data/data/com.termux/files/usr/etc/resolv.conf > $DEBIAN_PROOT_DIR/etc/resolv.conf
}

function _step_install_command {
SCRIPT="#!/data/data/com.termux/files/usr/bin/bash
cat /data/data/com.termux/files/usr/etc/resolv.conf > $DEBIAN_PROOT_DIR/etc/resolv.conf
proot-distro login debian -- su step -c bash -c 'bash /home/step/run-step.sh'
"
STEP_BIN="/data/data/com.termux/files/usr/bin/step"
echo "$SCRIPT" > $STEP_BIN
chmod +x $STEP_BIN
}


function _step_main_install {
  _step_check_for_existing_non_step_debian_proot
  _step_install_dependencies
  _step_attempt_homes_backup
  _step_dl_tarball
  _step_destroy_proot_distro
  _step_attempt_homes_restore
  _step_extract_tarball
  _step_apply_resolv_conf
  _step_install_command
  echo "finished!"
  echo "run with command: step"
}

_step_main_install
