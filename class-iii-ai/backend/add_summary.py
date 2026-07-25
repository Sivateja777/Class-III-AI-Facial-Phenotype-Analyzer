import pandas as pd
import os

filename = "QA_Test_Report.xlsx"
if not os.path.exists(filename):
    print("File not found")
    exit()

# Read the test cases
df = pd.read_excel(filename, sheet_name=0)

# Calculate metrics
total_tests = len(df)
passed = len(df[df['Status'].str.contains('Pass', case=False, na=False)])
failed = len(df[df['Status'].str.contains('Fail', case=False, na=False)])
pending = len(df[df['Status'] == 'Pending'])
manual = len(df[df['Automated'] == 'No'])
automated = len(df[df['Automated'] == 'Yes'])

summary_data = {
    "Metric": [
        "Total Test Cases", 
        "Passed Tests (Auto + Manual)", 
        "Failed Tests (Automated UI Errors)", 
        "Pending Tests", 
        "Manual Test Cases", 
        "Automated Test Cases"
    ],
    "Count": [total_tests, passed, failed, pending, manual, automated]
}

summary_df = pd.DataFrame(summary_data)

# Write to existing Excel file as a new sheet
with pd.ExcelWriter(filename, mode='a', engine='openpyxl', if_sheet_exists='replace') as writer:
    summary_df.to_excel(writer, sheet_name='Summary', index=False)

print("Summary added successfully.")
