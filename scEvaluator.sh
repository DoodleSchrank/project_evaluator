#!/bin/bash

EVAL_DIR=$(realpath ./sc_evaluation);
SC_LOCATION=$(realpath ./ScenarioCreator.jar);
SCENARIO_DISTANCE=0.8;
EVAL_LOC=$(realpath ./Evaluator.jar);

# ensure eval_dir exists
if [ ! -d "$EVAL_DIR" ]; then
  mkdir -p "$EVAL_DIR";
fi

COUNTER=1
while [ -d "$EVAL_DIR"/"$(printf "%03d\n" $COUNTER)" ]; do
  (( COUNTER++ ));
done
SUBDIR=$(printf "%03d\n" $COUNTER);
printf "starting with run #%s\n" "$SUBDIR";

mkdir -p "$EVAL_DIR"/"$SUBDIR";
mkdir -p "$EVAL_DIR"/"$SUBDIR"/schema;
mkdir -p "$EVAL_DIR"/"$SUBDIR"/eval;


echo "starting scenario creator...";

java -jar "$SC_LOCATION" -d $SCENARIO_DISTANCE -od "$EVAL_DIR"/"$SUBDIR"/schema;

echo "done ...";
echo "now starting the evaluation ...";

GT_YAML="$EVAL_DIR"/"$SUBDIR"/schema/groundTruth.yaml;
GT_CORRESPONDENCES="$EVAL_DIR"/"$SUBDIR"/schema/truthCorrespondences.yaml;
GT_CSV=$(find "$EVAL_DIR"/"$SUBDIR"/schema/ -type f -name 'gt_.*\.csv');


#truth Correspondences (YAML), truthYAML, truthCSV, alterationYAML, alterationCSV
EVAL_ARGS="-o $EVAL_DIR/$SUBDIR/eval/";
BASE_ALT_ARGS="-alt $GT_CORRESPONDENCES $GT_YAML $GT_CSV";


for alt in {00..99}; do
  yaml_file=$(find "$EVAL_DIR"/"$SUBDIR"/schema/ -type f -name "$alt\_.*\.ya?ml");
  # if no files, we done here
  if [ ! -e "$yaml_file" ]; then
      break;
  fi
  csv_files_list=$(find "$EVAL_DIR"/"$SUBDIR"/schema/ -type f -name "$alt\_.*\.csv");
  # if no files, we done here
  if (( ${#csv_files_list[@]} )); then
      break;
  fi
  csv_files="";
  for file in $csv_files_list; do
    csv_files="$csv_files,$file";
  done
  EVAL_ARGS="$EVAL_ARGS $BASE_ALT_ARGS $yaml_file $csv_files";
done

java -jar "$EVAL_LOC" "$EVAL_ARGS"

echo "done."