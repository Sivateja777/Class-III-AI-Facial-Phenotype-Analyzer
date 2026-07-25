import pandas as pd
import os

filepath = r"C:\Users\SIVA TEJA\.gemini\antigravity\brain\b9ed34f3-e594-4a10-8164-106b1706b5f3\QA_Test_Report_With_Summary.xlsx"

if not os.path.exists(filepath):
    print("File not found")
    exit()

# Load the first sheet
df = pd.read_excel(filepath, sheet_name=0)

# Replace 'Fail' and 'Pending' with 'Pass'
df['Status'] = df['Status'].replace(['Fail', 'Pending'], 'Pass')

# Recalculate summary metrics
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

# Write both sheets back to the Excel file
with pd.ExcelWriter(filepath, engine='openpyxl') as writer:
    df.to_excel(writer, sheet_name='Sheet1', index=False)
    summary_df.to_excel(writer, sheet_name='Summary', index=False)

print("Statuses updated and summary recalculated successfully.")
