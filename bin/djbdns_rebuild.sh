#!/bin/bash
#
# Put this script in the same directory as your djbdns data file, usually /etc/tinydns/root
#
# Break up your monolithic data file into one-file per domain, and put them in the "domains" subdirectory.
#
# When this script runs, it concatenates all files in the "domains" subdirectory into a monolithic data
# file and tried to build it via make. If that fails, the old file is kept.
#
# Note that you may still want to restart the tinydns service (svc -h /path/to/tinydns)
#

function die () {
  echo "${1}"
  exit 1
}

BASE=$(cd $(dirname $0) && pwd)

if [ ! -f data ] ; then
  echo "No data file found!"
  exit 1
fi

if [ ! -d ${BASE}/domains ] ; then
  echo "No domains dir found!"
  exit 1
fi

mkdir -p ${BASE}/backups || die "Error creating backups dir"

TODAY=$(date +%Y-%m-%d)
BACKUP=$(mktemp ${BASE}/backups/data.backup.${TODAY}.XXXXXXX)

cp data ${BACKUP} || die "Error backing up data file"
CHANGED=$(mktemp data.tried.${TODAY}.XXXXXX)
for domain in $(find ${BASE}/domains -type f) ; do
  echo "" >> ${CHANGED}
  echo "####################" >> ${CHANGED}
  echo "# $(basename ${domain})" >> ${CHANGED}
  echo "####################" >> ${CHANGED}
  cat ${domain} >> ${CHANGED}
  echo "" >> ${CHANGED}
done

mv ${CHANGED} data
make
if [ $? -ne 0 ] ; then
  echo "Error rebuilding data file, rolling back. Failed data file is in ${CHANGED}"
  mv data ${CHANGED}
  mv ${BACKUP} data
fi
