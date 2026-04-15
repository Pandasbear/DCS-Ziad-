package Client;

import Common.Employee;
import Common.UserRole;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class CrestGuiApp extends JFrame {
    private static final Color APP_BG = new Color(243, 246, 252);
    private static final Color PRIMARY = new Color(43, 102, 255);
    private static final Color OUTPUT_BG = new Color(22, 26, 33);
    private static final Color OUTPUT_FG = new Color(222, 228, 238);
    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_HR = "HR";
    private static final String CARD_STAFF = "STAFF";

    private final GuiServiceClient client = new GuiServiceClient();
    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private final JPanel root = new JPanel(cards);

    private final JTextField loginUserId = new JTextField();
    private final JPasswordField loginPassword = new JPasswordField();
    private final JLabel loginStatus = new JLabel("Ready");

    private final JLabel hrSessionLabel = new JLabel("HR Session");
    private final JTextArea hrOutput = new JTextArea();
    private final JTextField hrFirstName = new JTextField();
    private final JTextField hrLastName = new JTextField();
    private final JTextField hrIcPassport = new JTextField();
    private final JTextField hrInitPassword = new JTextField();
    private final JTextField hrRegisterPhone = new JTextField();
    private final JTextField hrRegisterEmergencyName = new JTextField();
    private final JTextField hrRegisterEmergencyNo = new JTextField();
    private final JTextField hrRegisterEmergencyRelationship = new JTextField();
    private final JTextField hrGeneratedEmployeeId = new JTextField();
    private final JTextField hrLeaveId = new JTextField();
    private final JTextField hrReportEmployeeId = new JTextField();
    private final JTextField hrReportYear = new JTextField();
    private final JTextField hrManageFilter = new JTextField();
    private final JTextField hrManageEmployeeId = new JTextField();
    private final JTextField hrManageFirstName = new JTextField();
    private final JTextField hrManageLastName = new JTextField();
    private final JTextField hrManageIcPassport = new JTextField();
    private final JTextField hrManagePhone = new JTextField();
    private final JTextField hrManageEmergencyName = new JTextField();
    private final JTextField hrManageEmergencyNo = new JTextField();
    private final JTextField hrManageEmergencyRelationship = new JTextField();

    private final JLabel staffSessionLabel = new JLabel("Staff Session");
    private final JTextArea staffOutput = new JTextArea();
    private final JTextField staffPhone = new JTextField();
    private final JTextField staffEmergencyName = new JTextField();
    private final JTextField staffEmergencyNo = new JTextField();
    private final JTextField staffEmergencyRelationship = new JTextField();
    private final JComboBox<String> staffLeaveType = new JComboBox<>(new String[]{"ANNUAL", "MEDICAL", "EMERGENCY", "UNPAID"});
    private final JComboBox<String> staffApplicationsFilter = new JComboBox<>(new String[]{"ALL", "PENDING", "APPROVED", "REJECTED"});
    private final JTextField staffStartDate = new JTextField();
    private final JTextField staffEndDate = new JTextField();
    private final JTextField staffReason = new JTextField();
    private final JLabel appStatus = new JLabel("Ready");

    public CrestGuiApp() {
        super("Crest HRM");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 760);
        setMinimumSize(new java.awt.Dimension(1000, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(APP_BG);

        root.add(buildLoginPanel(), CARD_LOGIN);
        root.add(buildHrPanel(), CARD_HR);
        root.add(buildStaffPanel(), CARD_STAFF);
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(root, BorderLayout.CENTER);

        appStatus.setBorder(new EmptyBorder(8, 12, 8, 12));
        appStatus.setOpaque(true);
        appStatus.setBackground(Color.WHITE);
        appStatus.setForeground(new Color(56, 62, 78));
        add(appStatus, BorderLayout.SOUTH);

        hrReportYear.setText(String.valueOf(LocalDate.now().getYear()));
        hrGeneratedEmployeeId.setEditable(false);
        hrGeneratedEmployeeId.setToolTipText("Auto-generated after registration");
        staffStartDate.setText(LocalDate.now().plusDays(1).toString());
        staffEndDate.setText(LocalDate.now().plusDays(1).toString());
        styleComponentTree(root);
    }

    private JPanel buildLoginPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(APP_BG);
        wrap.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        wrap.add(buildTitleBlock("Crest Leave Management", "Secure HR and staff workspace"), BorderLayout.NORTH);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Sign In"),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        GridBagConstraints c = baseGbc();
        c.gridx = 0; c.gridy = 0;
        card.add(new JLabel("User ID / Name"), c);
        c.gridx = 1;
        loginUserId.setColumns(20);
        card.add(loginUserId, c);

        c.gridx = 0; c.gridy = 1;
        card.add(new JLabel("Password"), c);
        c.gridx = 1;
        card.add(loginPassword, c);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> doLogin());
        c.gridx = 1; c.gridy = 2; c.anchor = GridBagConstraints.EAST;
        card.add(loginButton, c);

        loginStatus.setFont(loginStatus.getFont().deriveFont(Font.PLAIN));
        wrap.add(loginStatus, BorderLayout.SOUTH);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildHrPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(APP_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(buildTopBar(hrSessionLabel, () -> doLogoutToLogin()), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Register Employee", buildHrRegisterTab());
        tabs.addTab("Manage Employees", buildHrManageTab());
        tabs.addTab("Pending & Decision", buildHrDecisionTab());
        tabs.addTab("Yearly Report", buildHrReportTab());

        configureOutputArea(hrOutput);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, new JScrollPane(hrOutput));
        split.setResizeWeight(0.45);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHrRegisterTab() {
        JPanel p = createFormPanel();
        int y = 0;
        addRow(p, y++, "First Name", hrFirstName);
        addRow(p, y++, "Last Name", hrLastName);
        addRow(p, y++, "IC/Passport", hrIcPassport);
        addRow(p, y++, "Initial Password", hrInitPassword);
        addRow(p, y++, "Phone", hrRegisterPhone);
        addRow(p, y++, "Emergency Name", hrRegisterEmergencyName);
        addRow(p, y++, "Emergency Contact No", hrRegisterEmergencyNo);
        addRow(p, y++, "Emergency Relationship", hrRegisterEmergencyRelationship);
        addRow(p, y++, "Generated Employee ID", hrGeneratedEmployeeId);
        GridBagConstraints hint = baseGbc();
        hint.gridx = 1; hint.gridy = y++;
        p.add(new JLabel("8-64 chars with upper, lower, number, symbol"), hint);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton registerBtn = new JButton("Register Employee");
        registerBtn.addActionListener(e -> hrRegisterEmployee());
        JButton clearBtn = new JButton("Clear Output");
        clearBtn.addActionListener(e -> {
            hrOutput.setText("");
            clearHrRegisterFields();
        });
        buttons.add(registerBtn);
        buttons.add(clearBtn);

        GridBagConstraints c = baseGbc();
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
        p.add(buttons, c);
        return p;
    }

    private JPanel buildHrManageTab() {
        JPanel p = createFormPanel();
        int y = 0;
        addRow(p, y++, "Filter (ID/Name/IC)", hrManageFilter);
        addRow(p, y++, "Employee ID", hrManageEmployeeId);
        addRow(p, y++, "First Name", hrManageFirstName);
        addRow(p, y++, "Last Name", hrManageLastName);
        addRow(p, y++, "IC/Passport", hrManageIcPassport);
        addRow(p, y++, "Phone", hrManagePhone);
        addRow(p, y++, "Emergency Name", hrManageEmergencyName);
        addRow(p, y++, "Emergency Contact No", hrManageEmergencyNo);
        addRow(p, y++, "Emergency Relationship", hrManageEmergencyRelationship);

        JPanel buttons = new JPanel(new GridLayout(3, 2, 8, 8));
        JButton listBtn = new JButton("List All Employees");
        listBtn.addActionListener(e -> hrListEmployees());
        JButton loadBtn = new JButton("Load Employee");
        loadBtn.addActionListener(e -> hrLoadEmployee());
        JButton updateBtn = new JButton("Update Employee");
        updateBtn.addActionListener(e -> hrUpdateEmployee());
        JButton deleteBtn = new JButton("Delete Employee");
        deleteBtn.addActionListener(e -> hrDeleteEmployee());
        JButton clearFields = new JButton("Clear Fields");
        clearFields.addActionListener(e -> clearHrManageFields());
        JButton clearOutput = new JButton("Clear Output");
        clearOutput.addActionListener(e -> hrOutput.setText(""));
        buttons.add(listBtn);
        buttons.add(loadBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(clearFields);
        buttons.add(clearOutput);

        GridBagConstraints c = baseGbc();
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(buttons, c);
        return p;
    }

    private JPanel buildHrDecisionTab() {
        JPanel p = createFormPanel();
        int y = 0;
        addRow(p, y++, "Leave ID", hrLeaveId);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        JButton viewPending = new JButton("View Pending");
        viewPending.addActionListener(e -> hrShowPending());
        JButton approve = new JButton("Approve");
        approve.addActionListener(e -> hrDecideLeave(true));
        JButton reject = new JButton("Reject");
        reject.addActionListener(e -> hrDecideLeave(false));
        JButton clear = new JButton("Clear Output");
        clear.addActionListener(e -> hrOutput.setText(""));
        buttons.add(viewPending);
        buttons.add(approve);
        buttons.add(reject);
        buttons.add(clear);

        GridBagConstraints c = baseGbc();
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(buttons, c);
        return p;
    }

    private JPanel buildHrReportTab() {
        JPanel p = createFormPanel();
        int y = 0;
        addRow(p, y++, "Employee ID", hrReportEmployeeId);
        addRow(p, y++, "Year", hrReportYear);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reportBtn = new JButton("Generate Report");
        reportBtn.addActionListener(e -> hrGenerateReport());
        buttons.add(reportBtn);

        GridBagConstraints c = baseGbc();
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
        p.add(buttons, c);
        return p;
    }

    private JPanel buildStaffPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(APP_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(buildTopBar(staffSessionLabel, () -> doLogoutToLogin()), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Profile", buildStaffProfileTab());
        tabs.addTab("Leave", buildStaffLeaveTab());

        configureOutputArea(staffOutput);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, new JScrollPane(staffOutput));
        split.setResizeWeight(0.45);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStaffProfileTab() {
        JPanel p = createFormPanel();
        int y = 0;
        addRow(p, y++, "Phone", staffPhone);
        addRow(p, y++, "Emergency Name", staffEmergencyName);
        addRow(p, y++, "Emergency Contact No", staffEmergencyNo);
        addRow(p, y++, "Emergency Relationship", staffEmergencyRelationship);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        JButton loadBtn = new JButton("Load Profile");
        loadBtn.addActionListener(e -> staffLoadProfile());
        JButton updateBtn = new JButton("Update Details");
        updateBtn.addActionListener(e -> staffUpdateDetails());
        JButton balanceBtn = new JButton("View Balance");
        balanceBtn.addActionListener(e -> staffViewBalance());
        JButton clearBtn = new JButton("Clear Output");
        clearBtn.addActionListener(e -> staffOutput.setText(""));
        buttons.add(loadBtn);
        buttons.add(updateBtn);
        buttons.add(balanceBtn);
        buttons.add(clearBtn);

        GridBagConstraints c = baseGbc();
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(buttons, c);
        return p;
    }

    private JPanel buildStaffLeaveTab() {
        JPanel p = createFormPanel();
        int y = 0;
        addRow(p, y++, "Leave Type", staffLeaveType);
        addRow(p, y++, "Start Date (YYYY-MM-DD)", staffStartDate);
        addRow(p, y++, "End Date (YYYY-MM-DD)", staffEndDate);
        addRow(p, y++, "Reason", staffReason);
        addRow(p, y++, "Application Filter", staffApplicationsFilter);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        JButton applyBtn = new JButton("Apply Leave");
        applyBtn.addActionListener(e -> staffApplyLeave());
        JButton appsBtn = new JButton("View Applications");
        appsBtn.addActionListener(e -> staffViewApplications());
        JButton historyBtn = new JButton("View History");
        historyBtn.addActionListener(e -> staffViewHistory());
        JButton clearBtn = new JButton("Clear Output");
        clearBtn.addActionListener(e -> staffOutput.setText(""));
        buttons.add(applyBtn);
        buttons.add(appsBtn);
        buttons.add(historyBtn);
        buttons.add(clearBtn);

        GridBagConstraints c = baseGbc();
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(buttons, c);
        return p;
    }

    private JPanel buildTopBar(JLabel userLabel, Runnable onLogout) {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(219, 226, 239)),
                new EmptyBorder(4, 8, 4, 8)
        ));
        userLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> onLogout.run());
        top.add(userLabel, BorderLayout.WEST);
        top.add(logout, BorderLayout.EAST);
        return top;
    }

    private JPanel createFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(219, 226, 239)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return p;
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private void addRow(JPanel panel, int row, String label, java.awt.Component component) {
        GridBagConstraints c = baseGbc();
        c.gridx = 0; c.gridy = row; c.weightx = 0.2;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 0.8;
        panel.add(component, c);
    }

    private void configureOutputArea(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(false);
        area.setWrapStyleWord(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(OUTPUT_BG);
        area.setForeground(OUTPUT_FG);
        area.setBorder(BorderFactory.createTitledBorder("Output"));
    }

    private JPanel buildTitleBlock(String title, String subtitle) {
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(APP_BG);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setForeground(new Color(37, 47, 69));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(new Color(96, 110, 138));

        header.add(titleLabel);
        header.add(subtitleLabel);
        return header;
    }

    private void styleComponentTree(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                styleButton((JButton) component);
            } else if (component instanceof JTextField || component instanceof JPasswordField) {
                component.setBackground(Color.WHITE);
                if (component instanceof javax.swing.JComponent jc) {
                    jc.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(194, 205, 226)),
                            new EmptyBorder(6, 8, 6, 8)
                    ));
                }
            } else if (component instanceof JComboBox<?>) {
                component.setBackground(Color.WHITE);
            } else if (component instanceof JPanel panel) {
                styleComponentTree(panel);
            } else if (component instanceof JScrollPane scrollPane) {
                scrollPane.setBorder(BorderFactory.createLineBorder(new Color(194, 205, 226)));
            }
        }
    }

    private void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 12, 8, 12));
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
    }

    private void setStatus(String message) {
        appStatus.setText(message);
    }

    private void appendOutput(JTextArea area, String text) {
        String stamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        area.append("[" + stamp + "] " + text + "\n");
    }

    private void replaceOutput(JTextArea area, String text) {
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
    }

    private void showTable(JTextArea area, String title, String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = Arrays.stream(headers).mapToInt(String::length).toArray();
        for (String[] row : rows) {
            for (int i = 0; i < cols; i++) {
                String value = i < row.length ? nullSafe(row[i]) : "";
                widths[i] = Math.max(widths[i], value.length());
            }
        }

        StringBuilder sb = new StringBuilder("=== ").append(title).append(" ===\n");
        for (int i = 0; i < cols; i++) {
            sb.append(padRight(headers[i], widths[i] + 2));
        }
        sb.append('\n');
        for (int i = 0; i < cols; i++) {
            sb.append("-".repeat(widths[i])).append("  ");
        }
        sb.append('\n');
        for (String[] row : rows) {
            for (int i = 0; i < cols; i++) {
                String value = i < row.length ? nullSafe(row[i]) : "";
                sb.append(padRight(value, widths[i] + 2));
            }
            sb.append('\n');
        }
        replaceOutput(area, sb.toString());
    }

    private String filterLeaveOutput(String raw, String selectedFilter) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String filter = selectedFilter == null ? "ALL" : selectedFilter.trim().toUpperCase();
        if ("ALL".equals(filter)) {
            return raw;
        }
        StringBuilder sb = new StringBuilder();
        String[] lines = raw.split("\\R");
        for (String line : lines) {
            String upper = line.toUpperCase();
            if (!upper.contains("ID:")) {
                sb.append(line).append('\n');
                continue;
            }
            if (upper.contains("STATUS: " + filter)) {
                sb.append(line).append('\n');
            }
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? "No records for filter: " + filter : out;
    }

    private static String padRight(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text;
        return text + " ".repeat(width - text.length());
    }

    private void doLogin() {
        try {
            UserRole role = client.login(loginUserId.getText().trim(), new String(loginPassword.getPassword()));
            loginPassword.setText("");
            loginStatus.setText("Connected as " + client.currentUserId() + " (" + role + ")");
            setStatus("Signed in as " + client.currentUserId() + " (" + role + ")");
            if (role == UserRole.HR) {
                hrSessionLabel.setText("HR: " + client.currentUserId());
                cards.show(root, CARD_HR);
            } else {
                staffSessionLabel.setText("Staff: " + client.currentUserId());
                cards.show(root, CARD_STAFF);
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void doLogoutToLogin() {
        try {
            client.logout();
            cards.show(root, CARD_LOGIN);
            loginStatus.setText("Logged out.");
            setStatus("Logged out.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void hrRegisterEmployee() {
        try {
            String firstName = hrFirstName.getText().trim();
            String lastName = hrLastName.getText().trim();
            String icPassport = hrIcPassport.getText().trim();
            Employee e = client.registerEmployee(
                    firstName,
                    lastName,
                    icPassport,
                    hrInitPassword.getText().trim()
            );

            Employee enriched = new Employee(firstName, lastName, icPassport);
            enriched.setEmployeeId(e.getEmployeeId());
            enriched.setPhoneNo(normalizeBlank(hrRegisterPhone.getText()));
            enriched.setEmergencyName(normalizeBlank(hrRegisterEmergencyName.getText()));
            enriched.setEmergencyPhoneNo(normalizeBlank(hrRegisterEmergencyNo.getText()));
            enriched.setEmergencyRelationship(normalizeBlank(hrRegisterEmergencyRelationship.getText()));
            client.updateEmployeeByHr(enriched);

            hrGeneratedEmployeeId.setText(e.getEmployeeId());
            appendOutput(hrOutput, "Employee created: " + e.getEmployeeId());
            setStatus("Employee registered with details: " + e.getEmployeeId());
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void hrShowPending() {
        try {
            replaceOutput(hrOutput, client.viewPendingLeaveApplications());
            setStatus("Pending leave list refreshed.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void hrListEmployees() {
        try {
            List<Employee> all = client.listEmployees();
            String filter = normalizeBlank(hrManageFilter.getText());
            if (filter != null) {
                String f = filter.toLowerCase();
                all = all.stream().filter(e ->
                        nullSafe(e.getEmployeeId()).toLowerCase().contains(f)
                        || (nullSafe(e.getFirstName()) + " " + nullSafe(e.getLastName())).toLowerCase().contains(f)
                        || nullSafe(e.getIcPassport()).toLowerCase().contains(f)
                ).toList();
            }
            if (all.isEmpty()) {
                replaceOutput(hrOutput, "No employees matched the current filter.");
                return;
            }

            List<String[]> rows = new ArrayList<>();
            for (Employee e : all) {
                rows.add(new String[]{
                        nullSafe(e.getEmployeeId()),
                        (nullSafe(e.getFirstName()) + " " + nullSafe(e.getLastName())).trim(),
                        nullSafe(e.getIcPassport()),
                        nullSafe(e.getPhoneNo()),
                        nullSafe(e.getEmergencyName()),
                        nullSafe(e.getEmergencyRelationship()),
                        nullSafe(e.getEmergencyNo())
                });
            }
            showTable(hrOutput, "EMPLOYEES", new String[]{"ID", "NAME", "IC/PASSPORT", "PHONE", "EMERGENCY", "REL", "EMERGENCY NO"}, rows);
            setStatus("Listed " + all.size() + " employees.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void hrLoadEmployee() {
        try {
            Employee e = client.getEmployeeById(hrManageEmployeeId.getText().trim());
            hrManageEmployeeId.setText(nullSafe(e.getEmployeeId()));
            hrManageFirstName.setText(nullSafe(e.getFirstName()));
            hrManageLastName.setText(nullSafe(e.getLastName()));
            hrManageIcPassport.setText(nullSafe(e.getIcPassport()));
            hrManagePhone.setText(nullSafe(e.getPhoneNo()));
            hrManageEmergencyName.setText(nullSafe(e.getEmergencyName()));
            hrManageEmergencyNo.setText(nullSafe(e.getEmergencyNo()));
            hrManageEmergencyRelationship.setText(nullSafe(e.getEmergencyRelationship()));
            appendOutput(hrOutput, "Loaded employee: " + e.getEmployeeId());
            setStatus("Employee loaded: " + e.getEmployeeId());
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void hrUpdateEmployee() {
        try {
            Employee updated = new Employee(
                    hrManageFirstName.getText().trim(),
                    hrManageLastName.getText().trim(),
                    hrManageIcPassport.getText().trim()
            );
            updated.setEmployeeId(hrManageEmployeeId.getText().trim());
            updated.setPhoneNo(normalizeBlank(hrManagePhone.getText()));
            updated.setEmergencyName(normalizeBlank(hrManageEmergencyName.getText()));
            updated.setEmergencyPhoneNo(normalizeBlank(hrManageEmergencyNo.getText()));
            updated.setEmergencyRelationship(normalizeBlank(hrManageEmergencyRelationship.getText()));
            Employee saved = client.updateEmployeeByHr(updated);
            appendOutput(hrOutput, "Updated employee:\n" + formatEmployee(saved).trim());
            setStatus("Employee updated: " + saved.getEmployeeId());
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void hrDeleteEmployee() {
        try {
            String employeeId = hrManageEmployeeId.getText().trim();
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Delete employee " + employeeId + "? This removes leave history and account data.",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            client.deleteEmployee(employeeId);
            appendOutput(hrOutput, "Deleted employee: " + employeeId);
            setStatus("Employee deleted: " + employeeId);
            clearHrManageFields();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void hrDecideLeave(boolean approve) {
        try {
            int leaveId = Integer.parseInt(hrLeaveId.getText().trim());
            client.decideLeave(leaveId, approve);
            appendOutput(hrOutput, (approve ? "Approved " : "Rejected ") + "leave ID " + leaveId);
            setStatus("Leave " + leaveId + " " + (approve ? "approved." : "rejected."));
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void hrGenerateReport() {
        try {
            int year = Integer.parseInt(hrReportYear.getText().trim());
            String report = client.generateYearlyLeaveReport(hrReportEmployeeId.getText().trim(), year);
            appendOutput(hrOutput, report);
            setStatus("Yearly report generated.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void staffLoadProfile() {
        try {
            Employee profile = client.getMyProfile();
            staffPhone.setText(nullSafe(profile.getPhoneNo()));
            staffEmergencyName.setText(nullSafe(profile.getEmergencyName()));
            staffEmergencyNo.setText(nullSafe(profile.getEmergencyNo()));
            staffEmergencyRelationship.setText(nullSafe(profile.getEmergencyRelationship()));
            appendOutput(staffOutput, "Profile loaded for " + profile.getEmployeeId());
            setStatus("Profile loaded.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void staffUpdateDetails() {
        try {
            client.updateDetails(
                    normalizeBlank(staffPhone.getText()),
                    normalizeBlank(staffEmergencyName.getText()),
                    normalizeBlank(staffEmergencyNo.getText()),
                    normalizeBlank(staffEmergencyRelationship.getText())
            );
            appendOutput(staffOutput, "Details updated.");
            setStatus("Details saved.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void staffViewBalance() {
        try {
            int bal = client.leaveBalance();
            appendOutput(staffOutput, "Leave balance: " + bal);
            setStatus("Balance fetched.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void staffApplyLeave() {
        try {
            int leaveId = client.applyLeave(
                    (String) staffLeaveType.getSelectedItem(),
                    staffStartDate.getText().trim(),
                    staffEndDate.getText().trim(),
                    staffReason.getText().trim()
            );
            appendOutput(staffOutput, "Leave submitted. ID: " + leaveId);
            setStatus("Leave application submitted.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void staffViewApplications() {
        try {
            String raw = client.viewMyLeaveApplications();
            String filtered = filterLeaveOutput(raw, (String) staffApplicationsFilter.getSelectedItem());
            replaceOutput(staffOutput, filtered);
            setStatus("Applications refreshed.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void staffViewHistory() {
        try {
            String raw = client.viewMyLeaveHistory();
            String filtered = filterLeaveOutput(raw, (String) staffApplicationsFilter.getSelectedItem());
            replaceOutput(staffOutput, filtered);
            setStatus("History refreshed.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        setStatus("Action failed.");
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void clearHrManageFields() {
        hrManageEmployeeId.setText("");
        hrManageFirstName.setText("");
        hrManageLastName.setText("");
        hrManageIcPassport.setText("");
        hrManagePhone.setText("");
        hrManageEmergencyName.setText("");
        hrManageEmergencyNo.setText("");
        hrManageEmergencyRelationship.setText("");
    }

    private void clearHrRegisterFields() {
        hrFirstName.setText("");
        hrLastName.setText("");
        hrIcPassport.setText("");
        hrInitPassword.setText("");
        hrRegisterPhone.setText("");
        hrRegisterEmergencyName.setText("");
        hrRegisterEmergencyNo.setText("");
        hrRegisterEmergencyRelationship.setText("");
        hrGeneratedEmployeeId.setText("");
    }

    private static String normalizeBlank(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String formatEmployee(Employee e) {
        return "ID: " + nullSafe(e.getEmployeeId())
                + " | Name: " + nullSafe(e.getFirstName()) + " " + nullSafe(e.getLastName())
                + " | IC: " + nullSafe(e.getIcPassport())
                + " | Phone: " + nullSafe(e.getPhoneNo())
                + " | Emergency: " + nullSafe(e.getEmergencyName())
                + " (" + nullSafe(e.getEmergencyRelationship()) + ") "
                + nullSafe(e.getEmergencyNo())
                + "\n";
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new CrestGuiApp().setVisible(true));
    }
}
