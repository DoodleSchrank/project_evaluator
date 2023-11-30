#!/bin/sh
EVAL_DIR=$1;
# shellcheck disable=SC2034
SC_LOCATION=$2;
# shellcheck disable=SC2034
SCENARIO_DISTANCE=$3;
EVAL_LOC=$4;

COUNTER=1
while [ -d "$EVAL_DIR"/../"$(printf '%03d\n' $COUNTER)" ]; do
  ((COUNTER++));
done
SUBDIR=$(printf "%03d\n" "$COUNTER");
printf "starting with run #%s\n" "$SUBDIR";

mkdir -p "$EVAL_DIR/../$SUBDIR";
mkdir -p "$EVAL_DIR/../$SUBDIR"/schema;
mkdir -p "$EVAL_DIR/../$SUBDIR"/eval;

echo "starting scenario creator...";
for dataset in "$PWD"/"$EVAL_DIR"/*; do
  echo "dataset: $dataset";
  SCHEMA_PATH="$PWD/datafolder/$SUBDIR"/schema/"$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
  echo "av: $SCHEMA_PATH";
  mkdir -p $SCHEMA_PATH;
  java -jar --enable-preview "$SC_LOCATION" --no-tgd --körnerkissen -av $SCHEMA_PATH -ev "$dataset" --samen 1 > "$SCHEMA_PATH"/output.txt &
done
wait;

echo "done ...";
echo "now starting the evaluation ...";

for dataset in "$PWD"/datafolder/rawdata/*; do
  echo "evaluating:$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
  BASEPATH="$PWD"/datafolder/"$SUBDIR"/schema/"$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
  CORR_YAML=$(find "$BASEPATH" -type f -regex ".*correspondences\.ya?ml" | tr '\n' ',');
  SCHEMAS=$(find "$BASEPATH" -type f -regex ".*\/[0-9][0-9]\-[a-zA-Z].*\.yaml"  | tr '\n' ',');

  EVAL_ARGS="-o $EVAL_DIR/$SUBDIR/eval/";
  mkdir -p "$PWD/datafolder/$SUBDIR/eval/$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
  java -jar "$EVAL_LOC" -schemata "$SCHEMAS" -correspondences "$CORR_YAML" -av "$PWD/datafolder/$SUBDIR/eval/$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
done
echo "done."

