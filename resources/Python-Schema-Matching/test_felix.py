import schema_matching

df_pred, df_pred_labels, predicted_pairs = schema_matching("Test Data/QA/Table1.json", "Test Data/QA/Table2.json")
print(df_pred)
print(df_pred_labels)
for pair_tuple in predicted_pairs:
    print(pair_tuple)
