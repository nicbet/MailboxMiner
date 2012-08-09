#!/bin/bash

if [ "$#" -ne "5" ] ; then
  echo "Usage: $0 dir_with_gzipped_mbox_files database_url_and_port database_name user_name user_password"
  echo "Example: $0 mbox/ //mydb.local:5432 mm2_eclipse_list samantha ahtnamas"
  exit
fi

CWD=`pwd`
TMP=$CWD/tmp

DIR=$1
URL=$2
DB_NAME=$3
USER=$4
PASS=$5

BASE=`basename $DIR`

echo "Unpacking mbox files to ${TMP}"
rm -Rf $TMP
mkdir $TMP
for GZ in `ls ${DIR}/*.gz`;
do
    cat $GZ > $TMP/`basename $GZ`.mbox
done

echo "Creating database ${DB_NAME}"
java -Xmx4000M -jar $CWD/mailboxminer2.jar -connection $URL/ -username $USER -password $PASS -dbname $DB_NAME -module create -drop true

echo "Populating database ${DB_NAME}"
# -Djavax.activation.debug=true
java -Xmx4000M -jar $CWD/mailboxminer2.jar -connection $URL/$DB_NAME -username $USER -password $PASS -module insert -debug -logfile ${BASE}-insert.log -verbosity 3 -path $TMP
rm -Rf $TMP

echo "Unifying duplicate email addresses in database ${DB_NAME}"
java -Xmx4000M -jar $CWD/mailboxminer2.jar -connection $URL/$DB_NAME -username $USER -password $PASS -module persons -debug -logfile ${BASE}-persons.log -verbosity 3

echo "Recovering threads from database ${DB_NAME}"
java -Xmx4000M -jar $CWD/mailboxminer2.jar -connection $URL/$DB_NAME -username $USER -password $PASS -module threads -debug -logfile ${BASE}-threads.log -verbosity 3

rm -Rf $TMP