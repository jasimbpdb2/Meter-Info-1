package customerinfo.app;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import org.json.JSONArray;
import org.json.JSONObject;

public class UIHelper {
    private Context context;
    private TextView resultTextView;  // CHANGED FROM EditText TO TextView
    private LinearLayout tableContainer;

    public UIHelper(Context context, TextView resultTextView, LinearLayout tableContainer) {
        this.context = context;
        this.resultTextView = resultTextView;
        this.tableContainer = tableContainer;
    }

    // Rest of your methods...

    // Clear all UI content
    public void clearAll() {
        resultTextView.setText("");
        tableContainer.removeAllViews();
    }

    // Display text results with smooth formatting
    public void displayTextResult(String text) {
        resultTextView.setText(text);

        // Auto-scroll to top
        resultTextView.post(() -> {
            resultTextView.scrollTo(0, 0);
            resultTextView.clearFocus();
        });
    }

    // Display bill table with smooth scrolling
    public void displayBillTable(JSONArray billInfo) {
        try {
            if (billInfo.length() == 0) return;

            tableContainer.removeAllViews();

            // Create smooth scrolling container
            HorizontalScrollView horizontalScroll = createHorizontalScrollView();
            TableLayout tableLayout = createTableLayout();

            // Define table structure
            String[][] fields = {
                    {"Bill Month", "BILL_MONTH"},
                    {"Bill No", "BILL_NO"},
                    {"Consumption", "CONS_KWH_SR"},
                    {"CURRENT BILL", "CURRENT_BILL"},
                    {"Due Date", "INVOICE_DUE_DATE"},
                    {"Paid", "PAID_AMT"},
                    {"Pay Date", "RECEIPT_DATE"},
                    {"Balance", "BALANCE"}
            };

            int billCount = Math.min(billInfo.length(), 5);

            // Build table
            tableLayout.addView(createHeaderRow(billCount));

            for (int rowIndex = 0; rowIndex < fields.length; rowIndex++) {
                tableLayout.addView(createDataRow(fields[rowIndex], billInfo, billCount, rowIndex));

            }


            horizontalScroll.addView(tableLayout);
            tableContainer.addView(horizontalScroll);
            tableContainer.addView(createUsageHint());

        } catch (Exception e) {
            showError("Error creating table: " + e.getMessage());
        }
    }

    // Create smooth horizontal scroll view
    private HorizontalScrollView createHorizontalScrollView() {
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        scrollView.setScrollbarFadingEnabled(true);
        scrollView.setHorizontalScrollBarEnabled(true);
        scrollView.setSmoothScrollingEnabled(true);
        return scrollView;
    }

    // Create table layout with smooth properties
    private TableLayout createTableLayout() {
        TableLayout tableLayout = new TableLayout(context);
        tableLayout.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT
        ));
        tableLayout.setStretchAllColumns(true);
        tableLayout.setShrinkAllColumns(true);
        tableLayout.setBackgroundColor(Color.WHITE);
        tableLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        return tableLayout;
    }

    // Create header row
    private TableRow createHeaderRow(int billCount) {
        TableRow headerRow = new TableRow(context);
        headerRow.setBackgroundColor(Color.parseColor("#2c3e50"));

        // Field column
        headerRow.addView(createTableCell("BILL DETAILS", Gravity.START, Color.WHITE, true, true));

        // Bill columns
        for (int i = 0; i < billCount; i++) {
            headerRow.addView(createTableCell("BILL " + (i + 1), Gravity.CENTER, Color.WHITE, true, false));
        }

        return headerRow;
    }

    // Create data row
    private TableRow createDataRow(String[] field, JSONArray billInfo, int billCount, int rowIndex) {
        TableRow dataRow = new TableRow(context);

        // Alternate row colors
        dataRow.setBackgroundColor(rowIndex % 2 == 0 ? Color.WHITE : Color.parseColor("#f8f9fa"));

        String fieldName = field[0];
        String fieldKey = field[1];

        // Field name cell
        dataRow.addView(createTableCell(fieldName, Gravity.START, Color.BLACK, false, true));

        // Data cells
        for (int colIndex = 0; colIndex < billCount; colIndex++) {
            try {
                JSONObject bill = billInfo.getJSONObject(colIndex);
                String value = getFormattedBillValue(bill, fieldKey);

                // Handle empty pay dates
                if (fieldKey.equals("RECEIPT_DATE")) {
                    double paidAmount = bill.optDouble("PAID_AMT", 0);
                    if (paidAmount <= 0) value = "—";
                }

                TextView cell = createTableCell(value, Gravity.CENTER, Color.BLACK, false, false);

                // Add smooth selection
                setupCellSelection(cell, value);

                dataRow.addView(cell);
            } catch (Exception e) {
                dataRow.addView(createTableCell("N/A", Gravity.CENTER, Color.GRAY, false, false));
            }
        }

        return dataRow;
    }


    // Create table cell with smooth appearance
    private TextView createTableCell(String text, int gravity, int textColor, boolean isHeader, boolean isFieldColumn) {
        TextView textView = new TextView(context);
        textView.setText(text != null ? text : "");
        textView.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        textView.setGravity(gravity);
        textView.setTextColor(textColor);

        // Smooth text rendering
        textView.setTextIsSelectable(true);
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
        textView.setLongClickable(true);
        textView.setCursorVisible(false);
        textView.setSelectAllOnFocus(true);

        if (isHeader) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        }

        // Smooth background with rounded corners
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(6));

        if (isHeader) {
            background.setColor(Color.parseColor("#2c3e50")); // Dark blue
        } else if (isFieldColumn) {
            background.setColor(Color.parseColor("#ecf0f1")); // Light gray
        } else {
            background.setColor(Color.TRANSPARENT);
        }

        background.setStroke(dpToPx(1), Color.parseColor("#bdc3c7")); // Border
        textView.setBackground(background);

        return textView;
    }

    // Add this method to UIHelper class
    private void setupCellSelection(TextView cell, String value) {
        // Enable text selection action mode
        cell.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                // System will automatically add COPY, SELECT ALL, etc.
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                // Reset cell appearance when selection ends
                resetCellAppearance(cell);
            }
        });

        cell.setOnLongClickListener(v -> {
            // Visual feedback
            GradientDrawable selectedBg = new GradientDrawable();
            selectedBg.setCornerRadius(dpToPx(6));
            selectedBg.setColor(Color.parseColor("#d6eaf8"));
            selectedBg.setStroke(dpToPx(2), Color.parseColor("#3498db"));
            cell.setBackground(selectedBg);

            // Select all text automatically
            cell.setSelectAllOnFocus(true);
            cell.requestFocus();

            return false; // Let system handle the long press for selection
        });

        cell.setOnClickListener(v -> {
            // Clear selection when tapping elsewhere
            cell.clearFocus();
            resetCellAppearance(cell);
        });
    }
    // Reset cell to original appearance
    private void resetCellAppearance(TextView cell) {
        GradientDrawable originalBg = new GradientDrawable();
        originalBg.setCornerRadius(dpToPx(6));
        originalBg.setColor(Color.TRANSPARENT);
        originalBg.setStroke(dpToPx(1), Color.parseColor("#bdc3c7"));
        cell.setBackground(originalBg);
    }

    // Create usage hint
    private TextView createUsageHint() {
        TextView hint = new TextView(context);
        hint.setText("💡 Long press any cell to copy text • Scroll horizontally for more data");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hint.setTextColor(Color.parseColor("#7f8c8d"));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dpToPx(8), 0, dpToPx(4));
        return hint;
    }

    // Show error message
    private void showError(String message) {
        TextView errorText = new TextView(context);
        errorText.setText("❌ " + message);
        errorText.setTextColor(Color.RED);
        errorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        errorText.setPadding(0, dpToPx(8), 0, 0);
        tableContainer.addView(errorText);
    }

    // Format bill values (you can move this from MainActivity)
    private String getFormattedBillValue(JSONObject bill, String fieldKey) {
        try {
            if (!bill.has(fieldKey) || bill.isNull(fieldKey)) {
                return "—";
            }

            switch (fieldKey) {
                case "BILL_MONTH":
                    return formatBillMonth(bill.getString(fieldKey));

                case "INVOICE_DUE_DATE":
                case "RECEIPT_DATE":
                    return formatDate(bill.getString(fieldKey));

                case "CURRENT_BILL":
                case "ARREAR_BILL":
                case "TOTAL_BILL":
                case "PAID_AMT":
                case "BALANCE":
                    double amount = bill.getDouble(fieldKey);
                    return amount == 0 ? "—" : "৳" + String.format("%.0f", amount);

                case "CONS_KWH_SR":
                    double consumption = bill.getDouble(fieldKey);
                    return consumption == 0 ? "—" : String.format("%.0f", consumption);

                default:
                    return bill.getString(fieldKey);
            }
        } catch (Exception e) {
            return "—";
        }
    }

    // Date formatting helpers (move from MainActivity)
    private String formatBillMonth(String dateStr) {
        try {
            if (dateStr == null || dateStr.equals("null")) return "—";
            String[] parts = dateStr.substring(0, 10).split("-");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[1]);
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                return month >= 1 && month <= 12 ? monthNames[month-1] + " " + parts[0] : dateStr.substring(0,7);
            }
            return dateStr.length() >= 7 ? dateStr.substring(0,7) : dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null")) return "—";
        try {
            return dateString.contains("T") ? dateString.split("T")[0] : dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}