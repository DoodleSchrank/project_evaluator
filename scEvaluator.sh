#!/bin/bash
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

mkdir -p "$EVAL_DIR/../$SUBDIR";

for config in "max","--hetStructural 0.5 --hetLinguistic 0.3",false "mid","--hetStructural 0.3 --hetLinguistic 0.2",false "min","--hetStructural 0.1 --hetLinguistic 0.1",false "linkedSF","",true; do IFS=",";
  set -- $config;
  mkdir -p "$EVAL_DIR/../$SUBDIR"/schema-"$1";
  echo "starting scenario creator for $1...";
  for dataset in "$PWD"/"$EVAL_DIR"/*; do
    SCHEMA_PATH="$PWD"/datafolder/"$SUBDIR"/schema-$1/"$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
    mkdir -p "$SCHEMA_PATH";
    echo java -jar --enable-preview "$SC_LOCATION" "$2" --no-tgd --körnerkissen -av "$SCHEMA_PATH" -ev "$dataset" --samen 1 > "$SCHEMA_PATH"/output.txt;
    java -jar --enable-preview "$SC_LOCATION" $2 --no-tgd --körnerkissen -av "$SCHEMA_PATH" -ev "$dataset" --samen 1 > "$SCHEMA_PATH"/output.txt;
  done
  wait;
  echo "now starting the evaluation $1...";
  mkdir -p "$EVAL_DIR/../$SUBDIR/eval-$1";
  for dataset in "$PWD"/datafolder/rawdata/*; do
    echo "evaluating:$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
    BASEPATH="$PWD"/datafolder/"$SUBDIR"/schema-$1/"$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
    CORR_YAML=$(find "$BASEPATH" -type f -regex ".*correspondences\.ya?ml" | tr '\n' ',');
    SCHEMAS=$(find "$BASEPATH" -type f -regex ".*\/[0-9][0-9]\-[a-zA-Z].*\.yaml"  | tr '\n' ',');
    mkdir -p "$PWD/datafolder/$SUBDIR/eval-$1/$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)";
    java -jar "$EVAL_LOC" -schemata "$SCHEMAS" -correspondences "$CORR_YAML" -av "$PWD/datafolder/$SUBDIR/eval-$1/$(echo "$dataset" | rev | cut -d'/' -f-1 | rev)" -linkedSF $3;
  done
done

mkdir -p "$EVAL_DIR/../$SUBDIR"/schema-singleT-struct;
echo "starting scenario creator for singleT-struct...";
SCHEMA_PATH="$PWD"/datafolder/"$SUBDIR"/schema-singleT-struct/ccs;
mkdir -p "$SCHEMA_PATH";
java -jar --enable-preview "$SC_LOCATION" --einzelausführung TableToColumnLeafs --no-tgd --körnerkissen -av "$SCHEMA_PATH" -ev "$dataset" --samen 1 > "$SCHEMA_PATH"/output.txt;

echo "now starting the evaluation singleT-struct...";
mkdir -p "$EVAL_DIR/../$SUBDIR"/eval-singleT-struct;
BASEPATH="$PWD"/datafolder/"$SUBDIR"/schema-singleT-struct/ccs;
CORR_YAML=$(find "$BASEPATH" -type f -regex ".*correspondences\.ya?ml" | tr '\n' ',');
SCHEMAS=$(find "$BASEPATH" -type f -regex ".*\/[0-9][0-9]\-[a-zA-Z].*\.yaml"  | tr '\n' ',');
mkdir -p "$PWD/datafolder/$SUBDIR"/eval-singleT-struct/ccs;
java -jar "$EVAL_LOC" -schemata "$SCHEMAS" -correspondences "$CORR_YAML" -av "$PWD/datafolder/$SUBDIR"/eval-singleT-struct/ccs -linkedSF false;



mkdir -p "$EVAL_DIR/../$SUBDIR"/schema-singleT-ling;
echo "starting scenario creator for singleT-ling...";
SCHEMA_PATH="$PWD"/datafolder/"$SUBDIR"/schema-singleT-ling/ccs;
mkdir -p "$SCHEMA_PATH";
java -jar --enable-preview "$SC_LOCATION" --einzelausführung ChangeLanguageOfColumnName --no-tgd --körnerkissen -av "$SCHEMA_PATH" -ev "$PWD"/"$EVAL_DIR"/ccs --samen 1 > "$SCHEMA_PATH"/output.txt;

echo "now starting the evaluation singleT-ling...";
mkdir -p "$EVAL_DIR/../$SUBDIR"/eval-singleT-ling;
BASEPATH="$PWD"/datafolder/"$SUBDIR"/schema-singleT-ling/ccs;
CORR_YAML=$(find "$BASEPATH" -type f -regex ".*correspondences\.ya?ml" | tr '\n' ',');
SCHEMAS=$(find "$BASEPATH" -type f -regex ".*\/[0-9][0-9]\-[a-zA-Z].*\.yaml"  | tr '\n' ',');
mkdir -p "$PWD/datafolder/$SUBDIR"/eval-singleT-ling/ccs;
java -jar "$EVAL_LOC" -schemata "$SCHEMAS" -correspondences "$CORR_YAML" -av "$PWD/datafolder/$SUBDIR"/eval-singleT-ling/ccs -linkedSF false;

echo "done ...";

