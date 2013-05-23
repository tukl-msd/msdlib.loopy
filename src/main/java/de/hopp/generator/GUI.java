package de.hopp.generator;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.hopp.generator.backends.server.virtex6.ProjectBackend;
import de.hopp.generator.frontend.ClientBackend;
import de.hopp.generator.frontend.ServerBackend;

public class GUI {
    private final Main main;
    private Configuration config;

    private JFrame frmTitle;
    private final ButtonGroup logLevelGroup = new ButtonGroup();
    private JTextField bdlFileTextField;
    private JComboBox project;
    private final JFileChooser bdlFileChooser = new JFileChooser();
    private final JFileChooser hostDirFileChooser = new JFileChooser();
    private final JFileChooser boardDirFileChooser = new JFileChooser();
    private final JFileChooser tempDirFileChooser = new JFileChooser();
    private JPanel fileSelectPanel;

    private File bdlFile;
    private JTextField hostDirTextField;
    private JTextField boardDirTextField;
    private JTextField tempDirTextField;

    /**
     * Launch the application.
     */
    public static void run(final Main main) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    GUI window = new GUI(main);
                    window.frmTitle.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public GUI(Main main) {
        this.main = main;
        this.config = main.config();
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmTitle = new JFrame();
        frmTitle.setTitle("Loopy Driver Generator");
        frmTitle.setBounds(500, 500, 325, 500);
        frmTitle.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
        frmTitle.getContentPane().setLayout(gridBagLayout);

        fileSelectPanel = new JPanel();
        fileSelectPanel.setBorder(new TitledBorder(null, "BDL File", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_fileSelectPanel = new GridBagConstraints();
        gbc_fileSelectPanel.insets = new Insets(0, 0, 5, 0);
        gbc_fileSelectPanel.fill = GridBagConstraints.BOTH;
        gbc_fileSelectPanel.gridx = 0;
        gbc_fileSelectPanel.gridy = 0;
        frmTitle.getContentPane().add(fileSelectPanel, gbc_fileSelectPanel);
        GridBagLayout gbl_fileSelectPanel = new GridBagLayout();
        gbl_fileSelectPanel.columnWidths = new int[]{0, 0, 0};
        gbl_fileSelectPanel.rowHeights = new int[]{0, 0};
        gbl_fileSelectPanel.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
        gbl_fileSelectPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
        fileSelectPanel.setLayout(gbl_fileSelectPanel);

        bdlFileTextField = new JTextField();
        bdlFileTextField.setToolTipText("Select BDL file to be parsed here");
//        bdlFileTextField.setText(config.bdlFile().getAbsolutePath());
//        bdlFileTextField.getDocument().addDocumentListener(new DocumentListener() {
//            public void removeUpdate(DocumentEvent arg0)  { contentChanged(); }
//            public void insertUpdate(DocumentEvent arg0)  { contentChanged(); }
//            public void changedUpdate(DocumentEvent arg0) { contentChanged(); }
//            private void contentChanged() {
//                config.setBDLFile(new File(bdlFileTextField.getText()));
//            }
//        });
        GridBagConstraints gbc_bdlFileTextField = new GridBagConstraints();
        gbc_bdlFileTextField.fill = GridBagConstraints.HORIZONTAL;
        gbc_bdlFileTextField.insets = new Insets(0, 0, 0, 5);
        gbc_bdlFileTextField.gridx = 0;
        gbc_bdlFileTextField.gridy = 0;
        fileSelectPanel.add(bdlFileTextField, gbc_bdlFileTextField);
        bdlFileTextField.setColumns(10);

        JButton bdlFileSelect = new JButton("Select");
        bdlFileSelect.setToolTipText("Opens a graphical file selector");
        bdlFileSelect.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                bdlFileChooser.showOpenDialog(fileSelectPanel);
//                if (bdlFileChooser.showOpenDialog(fileSelectPanel) == JFileChooser.APPROVE_OPTION) {
////                    bdlFile = bdlFileChooser.getSelectedFile();
////                    bdlFileTextField.setText(bdlFile.getPath());
//                }
            }
        });
        GridBagConstraints gbc_bdlFileSelect = new GridBagConstraints();
        gbc_bdlFileSelect.gridx = 1;
        gbc_bdlFileSelect.gridy = 0;
        fileSelectPanel.add(bdlFileSelect, gbc_bdlFileSelect);

        JPanel backendPanel = new JPanel();
        backendPanel.setBorder(new TitledBorder(null, "Backends", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_backendPanel = new GridBagConstraints();
        gbc_backendPanel.fill = GridBagConstraints.BOTH;
        gbc_backendPanel.insets = new Insets(0, 0, 5, 0);
        gbc_backendPanel.gridx = 0;
        gbc_backendPanel.gridy = 1;
        frmTitle.getContentPane().add(backendPanel, gbc_backendPanel);
        GridBagLayout gbl_backendPanel = new GridBagLayout();
        gbl_backendPanel.columnWidths = new int[]{0, 0, 0};
        gbl_backendPanel.rowHeights = new int[]{0, 0, 0, 0};
        gbl_backendPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_backendPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
        backendPanel.setLayout(gbl_backendPanel);

        JLabel lblHostBackend = new JLabel("Host Backend");
        GridBagConstraints gbc_lblHostBackend = new GridBagConstraints();
        gbc_lblHostBackend.insets = new Insets(0, 0, 5, 5);
        gbc_lblHostBackend.gridx = 0;
        gbc_lblHostBackend.gridy = 0;
        backendPanel.add(lblHostBackend, gbc_lblHostBackend);

        JComboBox host = new JComboBox();
        host.setToolTipText("Select the target language, for which a host-side driver should be generated");
        GridBagConstraints gbc_host = new GridBagConstraints();
        gbc_host.fill = GridBagConstraints.HORIZONTAL;
        gbc_host.insets = new Insets(0, 0, 5, 0);
        gbc_host.gridx = 1;
        gbc_host.gridy = 0;
        backendPanel.add(host, gbc_host);
        DefaultComboBoxModel hostModel = new DefaultComboBoxModel(ClientBackend.values());
        hostModel.insertElementAt("<None>", 0);
        host.setModel(hostModel);
        host.setName("Host Backend");

        if(config.client() == null) host.setSelectedIndex(0);
        else host.setSelectedItem(config.client());

        host.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                // ignore deselection events
                if(arg0.getStateChange() == ItemEvent.DESELECTED) return;

                if(arg0.getItem().equals("<None>")) config.setClient(null);
                else config.setClient(((ClientBackend)arg0.getItem()).getInstance());
            }
        });

        JLabel lblBoardBackend = new JLabel("Board Backend");
        GridBagConstraints gbc_lblBoardBackend = new GridBagConstraints();
        gbc_lblBoardBackend.insets = new Insets(0, 0, 5, 5);
        gbc_lblBoardBackend.gridx = 0;
        gbc_lblBoardBackend.gridy = 1;
        backendPanel.add(lblBoardBackend, gbc_lblBoardBackend);

        JComboBox board = new JComboBox();
        board.setToolTipText("Select the target board architecture, for which a driver should be generated");
        GridBagConstraints gbc_board = new GridBagConstraints();
        gbc_board.fill = GridBagConstraints.HORIZONTAL;
        gbc_board.insets = new Insets(0, 0, 5, 0);
        gbc_board.gridx = 1;
        gbc_board.gridy = 1;
        backendPanel.add(board, gbc_board);
        DefaultComboBoxModel boardModel = new DefaultComboBoxModel(ServerBackend.values());
        boardModel.insertElementAt("<None>", 0);
        board.setModel(boardModel);
        board.setName("Board Backend");

        if(config.server() == null) board.setSelectedIndex(0);
        else board.setSelectedItem(config.server());

        board.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                // ignore deselection events
                if(arg0.getStateChange() == ItemEvent.DESELECTED) return;

                if(arg0.getItem().equals("<None>")) {
                    config.setServer(null);
                    project.setEnabled(false);
                }
                else {
                    config.setServer(((ServerBackend)arg0.getItem()).getInstance());
                    // change items of project pulldown accordingly
                    getProject().removeAllItems();
                    for(ProjectBackend b : ProjectBackend.values())
                        getProject().addItem(b);
                    project.setEnabled(true);
                }
            }
        });
        JLabel lblProjectBackend = new JLabel("Project Backend");
        GridBagConstraints gbc_lblProjectBackend = new GridBagConstraints();
        gbc_lblProjectBackend.anchor = GridBagConstraints.EAST;
        gbc_lblProjectBackend.insets = new Insets(0, 0, 0, 5);
        gbc_lblProjectBackend.gridx = 0;
        gbc_lblProjectBackend.gridy = 2;
        backendPanel.add(lblProjectBackend, gbc_lblProjectBackend);

        project = new JComboBox();
        GridBagConstraints gbc_project = new GridBagConstraints();
        gbc_project.fill = GridBagConstraints.HORIZONTAL;
        gbc_project.gridx = 1;
        gbc_project.gridy = 2;
        backendPanel.add(project, gbc_project);
        project.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                // ignore deselection events
                if(arg0.getStateChange() == ItemEvent.DESELECTED) return;

                if(arg0.getItem().equals("<None>")) config.setServer(null);
            }
        });
        project.setToolTipText("Select the workflow used to generate the board-side driver");

        if(board.getSelectedIndex() == 0) project.setEnabled(false);
        else if(board.getSelectedItem().equals(ServerBackend.VIRTEX6))
            project.setModel(new DefaultComboBoxModel(ProjectBackend.values()));


        final JPanel directoryPanel = new JPanel();
        directoryPanel.setBorder(new TitledBorder(null, "Directories", TitledBorder.LEFT, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_directoryPanel = new GridBagConstraints();
        gbc_directoryPanel.anchor = GridBagConstraints.NORTH;
        gbc_directoryPanel.insets = new Insets(0, 0, 5, 0);
        gbc_directoryPanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_directoryPanel.gridx = 0;
        gbc_directoryPanel.gridy = 2;
        frmTitle.getContentPane().add(directoryPanel, gbc_directoryPanel);
        GridBagLayout gbl_directoryPanel = new GridBagLayout();
        gbl_directoryPanel.columnWidths = new int[]{0, 0, 0};
        gbl_directoryPanel.rowHeights = new int[]{0, 0, 0, 0};
        gbl_directoryPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_directoryPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
        directoryPanel.setLayout(gbl_directoryPanel);

        JLabel lblHost = new JLabel("Host");
        GridBagConstraints gbc_lblHost = new GridBagConstraints();
        gbc_lblHost.anchor = GridBagConstraints.EAST;
        gbc_lblHost.insets = new Insets(0, 0, 5, 5);
        gbc_lblHost.gridx = 0;
        gbc_lblHost.gridy = 0;
        directoryPanel.add(lblHost, gbc_lblHost);

        hostDirTextField = new JTextField();
        hostDirTextField.setToolTipText("Select the target directory for host-side driver files");
        GridBagConstraints gbc_hostDirTextField = new GridBagConstraints();
        gbc_hostDirTextField.insets = new Insets(0, 0, 5, 5);
        gbc_hostDirTextField.fill = GridBagConstraints.HORIZONTAL;
        gbc_hostDirTextField.gridx = 1;
        gbc_hostDirTextField.gridy = 0;
        directoryPanel.add(hostDirTextField, gbc_hostDirTextField);
        hostDirTextField.setColumns(10);
        hostDirTextField.setText(config.clientDir().getAbsolutePath());
        hostDirTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent arg0)  { contentChanged(); }
            public void insertUpdate(DocumentEvent arg0)  { contentChanged(); }
            public void changedUpdate(DocumentEvent arg0) { contentChanged(); }
            private void contentChanged() {
                config.setClientDir(new File(hostDirTextField.getText()));
            }
        });

        hostDirFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JButton hostDirSelect = new JButton("Select");
        hostDirSelect.setToolTipText("Opens a graphical file selector");
        hostDirSelect.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (hostDirFileChooser.showOpenDialog(directoryPanel) == JFileChooser.APPROVE_OPTION) {
                    config.setClientDir(hostDirFileChooser.getSelectedFile());
                    hostDirTextField.setText(config.clientDir().getPath());
                }
            }
        });
        GridBagConstraints gbc_hostDirSelect = new GridBagConstraints();
        gbc_hostDirSelect.insets = new Insets(0, 0, 5, 0);
        gbc_hostDirSelect.gridx = 2;
        gbc_hostDirSelect.gridy = 0;
        directoryPanel.add(hostDirSelect, gbc_hostDirSelect);

        JLabel lblBoard = new JLabel("Board");
        GridBagConstraints gbc_lblBoard = new GridBagConstraints();
        gbc_lblBoard.anchor = GridBagConstraints.EAST;
        gbc_lblBoard.insets = new Insets(0, 0, 5, 5);
        gbc_lblBoard.gridx = 0;
        gbc_lblBoard.gridy = 1;
        directoryPanel.add(lblBoard, gbc_lblBoard);

        boardDirTextField = new JTextField();
        boardDirTextField.setToolTipText("Select the target directory for board-side driver files");
        GridBagConstraints gbc_boardDirTextField = new GridBagConstraints();
        gbc_boardDirTextField.insets = new Insets(0, 0, 5, 5);
        gbc_boardDirTextField.fill = GridBagConstraints.HORIZONTAL;
        gbc_boardDirTextField.gridx = 1;
        gbc_boardDirTextField.gridy = 1;
        directoryPanel.add(boardDirTextField, gbc_boardDirTextField);
        boardDirTextField.setColumns(10);
        boardDirTextField.setText(config.serverDir().getAbsolutePath());
        boardDirTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent arg0)  { contentChanged(); }
            public void insertUpdate(DocumentEvent arg0)  { contentChanged(); }
            public void changedUpdate(DocumentEvent arg0) { contentChanged(); }
            private void contentChanged() {
                config.setServerDir(new File(boardDirTextField.getText()));
            }
        });

        boardDirFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JButton boardDirSelect = new JButton("Select");
        boardDirSelect.setToolTipText("Opens a graphical file selector");
        boardDirSelect.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (boardDirFileChooser.showOpenDialog(directoryPanel) == JFileChooser.APPROVE_OPTION) {
                    config.setServerDir(boardDirFileChooser.getSelectedFile());
                    boardDirTextField.setText(config.serverDir().getPath());
                }
            }
        });
        GridBagConstraints gbc_boardDirSelect = new GridBagConstraints();
        gbc_boardDirSelect.insets = new Insets(0, 0, 5, 0);
        gbc_boardDirSelect.gridx = 2;
        gbc_boardDirSelect.gridy = 1;
        directoryPanel.add(boardDirSelect, gbc_boardDirSelect);

        JLabel lblTemp = new JLabel("Temp");
        GridBagConstraints gbc_lblTemp = new GridBagConstraints();
        gbc_lblTemp.anchor = GridBagConstraints.EAST;
        gbc_lblTemp.insets = new Insets(0, 0, 0, 5);
        gbc_lblTemp.gridx = 0;
        gbc_lblTemp.gridy = 2;
        directoryPanel.add(lblTemp, gbc_lblTemp);

        tempDirTextField = new JTextField();
        tempDirTextField.setToolTipText("Select the target directory for temporary files");

        GridBagConstraints gbc_tempDirTextField = new GridBagConstraints();
        gbc_tempDirTextField.insets = new Insets(0, 0, 0, 5);
        gbc_tempDirTextField.fill = GridBagConstraints.HORIZONTAL;
        gbc_tempDirTextField.gridx = 1;
        gbc_tempDirTextField.gridy = 2;
        directoryPanel.add(tempDirTextField, gbc_tempDirTextField);
        tempDirTextField.setColumns(10);
        tempDirTextField.setText(config.tempDir().getAbsolutePath());
        tempDirTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent arg0)  { contentChanged(); }
            public void insertUpdate(DocumentEvent arg0)  { contentChanged(); }
            public void changedUpdate(DocumentEvent arg0) { contentChanged(); }
            private void contentChanged() {
                config.setTempDir(new File(tempDirTextField.getText()));
            }
        });

        tempDirFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JButton tempDirSelect = new JButton("Select");
        tempDirSelect.setToolTipText("Opens a graphical file selector");
        tempDirSelect.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (tempDirFileChooser.showOpenDialog(directoryPanel) == JFileChooser.APPROVE_OPTION) {
                    config.setTempDir(tempDirFileChooser.getSelectedFile());
                    tempDirTextField.setText(config.tempDir().getPath());
                }
            }
        });
        GridBagConstraints gbc_tempDirSelect = new GridBagConstraints();
        gbc_tempDirSelect.gridx = 2;
        gbc_tempDirSelect.gridy = 2;
        directoryPanel.add(tempDirSelect, gbc_tempDirSelect);

        JPanel logLevelPanel = new JPanel();
        logLevelPanel.setBorder(new TitledBorder(null, "Logging Level", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        logLevelPanel.setName("Name");
        GridBagConstraints gbc_logLevelPanel = new GridBagConstraints();
        gbc_logLevelPanel.anchor = GridBagConstraints.NORTH;
        gbc_logLevelPanel.insets = new Insets(0, 0, 5, 0);
        gbc_logLevelPanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_logLevelPanel.gridx = 0;
        gbc_logLevelPanel.gridy = 3;
        frmTitle.getContentPane().add(logLevelPanel, gbc_logLevelPanel);
        GridBagLayout gbl_logLevelPanel = new GridBagLayout();
        gbl_logLevelPanel.columnWidths = new int[]{0, 0, 0};
        gbl_logLevelPanel.rowHeights = new int[]{0, 0, 0};
        gbl_logLevelPanel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
        gbl_logLevelPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
        logLevelPanel.setLayout(gbl_logLevelPanel);

        JRadioButton logLevelQuiet = new JRadioButton("Quiet");
        logLevelQuiet.setToolTipText("Set the logging level to quiet, resulting in no console output at all");
        logLevelQuiet.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED)
                    config.setLogQuiet();
            }
        });
        logLevelGroup.add(logLevelQuiet);
        GridBagConstraints gbc_logLevelQuiet = new GridBagConstraints();
        gbc_logLevelQuiet.insets = new Insets(0, 0, 5, 5);
        gbc_logLevelQuiet.anchor = GridBagConstraints.WEST;
        gbc_logLevelQuiet.gridx = 0;
        gbc_logLevelQuiet.gridy = 0;
        logLevelPanel.add(logLevelQuiet, gbc_logLevelQuiet);

        JRadioButton logLevelVerbose = new JRadioButton("Verbose");
        logLevelVerbose.setToolTipText("Set the logging level to verbose, resulting in enhanced console output");
        logLevelVerbose.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED)
                    config.setLogVerbose();
            }
        });
        logLevelGroup.add(logLevelVerbose);
        GridBagConstraints gbc_logLevelVerbose = new GridBagConstraints();
        gbc_logLevelVerbose.insets = new Insets(0, 0, 5, 0);
        gbc_logLevelVerbose.anchor = GridBagConstraints.WEST;
        gbc_logLevelVerbose.gridx = 1;
        gbc_logLevelVerbose.gridy = 0;
        logLevelPanel.add(logLevelVerbose, gbc_logLevelVerbose);

        JRadioButton logLevelInfo = new JRadioButton("Info");
        logLevelInfo.setToolTipText("Set the logging level to info, resulting in some console output");
        logLevelInfo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED)
                    config.setLogInfo();
            }
        });
        logLevelGroup.add(logLevelInfo);
        GridBagConstraints gbc_logLevelInfo = new GridBagConstraints();
        gbc_logLevelInfo.insets = new Insets(0, 0, 5, 5);
        gbc_logLevelInfo.anchor = GridBagConstraints.WEST;
        gbc_logLevelInfo.gridx = 0;
        gbc_logLevelInfo.gridy = 1;
        logLevelPanel.add(logLevelInfo, gbc_logLevelInfo);

        JRadioButton logLevelDebug = new JRadioButton("Debug");
        logLevelDebug.setToolTipText("Set the logging level to debug, resulting in a lot of console output for the purpose of generator debugging");
        logLevelDebug.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED)
                    config.setLogDebug();
            }
        });
        logLevelGroup.add(logLevelDebug);
        GridBagConstraints gbc_logLevelDebug = new GridBagConstraints();
        gbc_logLevelDebug.insets = new Insets(0, 0, 5, 0);
        gbc_logLevelDebug.anchor = GridBagConstraints.WEST;
        gbc_logLevelDebug.gridx = 1;
        gbc_logLevelDebug.gridy = 1;
        logLevelPanel.add(logLevelDebug, gbc_logLevelDebug);

        config.printConfig();
             if(config.DEBUG())   logLevelDebug.setSelected(true);
        else if(config.VERBOSE()) logLevelVerbose.setSelected(true);
        else if(config.INFO())    logLevelInfo.setSelected(true);
        else if(config.QUIET())   logLevelQuiet.setSelected(true);

        JButton btnRun = new JButton("Run");
        btnRun.setToolTipText("Execute the driver generator with the provided configuration");
        btnRun.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                main.run();
            }
        });

        btnRun.setVerticalAlignment(SwingConstants.BOTTOM);
        GridBagConstraints gbc_btnRun = new GridBagConstraints();
        gbc_btnRun.anchor = GridBagConstraints.SOUTH;
        gbc_btnRun.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnRun.gridx = 0;
        gbc_btnRun.gridy = 4;
        frmTitle.getContentPane().add(btnRun, gbc_btnRun);

        JMenuBar menuBar = new JMenuBar();
        frmTitle.setJMenuBar(menuBar);

        JMenu menuFile = new JMenu("File");
        menuBar.add(menuFile);

        JMenuItem menuFileNew = new JMenuItem("New");
        menuFileNew.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                config = new Configuration();
            }
        });
        menuFileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        menuFileNew.setToolTipText("Create a new run configuration.");
        menuFile.add(menuFileNew);

        JMenuItem menuFileOpen = new JMenuItem("Open");
        menuFileOpen.setToolTipText("Open an existing configuration. Note, that configurations are NOT portable!");
        menuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        menuFile.add(menuFileOpen);

        JSeparator separator = new JSeparator();
        menuFile.add(separator);

        JMenuItem menuFileSave = new JMenuItem("Save");
        menuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        menuFileSave.setToolTipText("Saves this configuration. Note, that configurations are NOT portable!");
        menuFile.add(menuFileSave);

        JMenuItem menuFileSaveAs = new JMenuItem("Save As...");
        menuFileSaveAs.setToolTipText("Save this configuration in a new file. Note, that configurations are NOT portable!");
        menuFile.add(menuFileSaveAs);

        JSeparator separator_1 = new JSeparator();
        menuFile.add(separator_1);

        JMenuItem menuFileQuit = new JMenuItem("Quit");
        menuFileQuit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.exit(0);
            }
        });
        menuFileQuit.setToolTipText("Exit this petty excuse for a graphical interface!");
        menuFileQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        menuFile.add(menuFileQuit);
        initDataBindings();
    }
    protected JComboBox getProject() {
        return project;
    }
    protected JPanel getFileSelectPanel() {
        return fileSelectPanel;
    }
    protected void initDataBindings() {
    }
}
