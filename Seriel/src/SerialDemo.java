/*
 * @(#)SerialDemo.java	1.9 98/06/05 SMI
 *
 * Copyright (c) 1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license
 * to use, modify and redistribute this software in source and binary
 * code form, provided that i) this copyright notice and license appear
 * on all copies of the software; and ii) Licensee does not utilize the
 * software in a manner which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND
 * ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY
 * LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THE
 * SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS
 * BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING
 * OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control
 * of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.
 */

import javax.comm.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Enumeration;

/**
 * Main file for SerialDemo program. This program illustrates many of the
 * abilities of the javax.comm api. This file contains the GUI framework that
 * the program runs in.
 */

public class SerialDemo extends Frame implements ActionListener {

	final int HEIGHT = 650;
	final int WIDTH = 610;
	private MenuBar mb;
	private Menu fileMenu;
	private MenuItem openItem;
	private MenuItem saveItem;
	private MenuItem exitItem;

	private Button openButton;
	private Button closeButton;
	private Button breakButton;
	private Button plotButton;
	private Button StopButton;
	private Panel buttonPanel;

	private Panel messagePanel;
	private Panel graphicPanel;
	private TextArea messageAreaOut;
	
	private TextArea messageAreaOutT1;//
	private TextArea messageAreaOutR2;//
	private TextArea messageAreaOutT2;//
	private Label MSR1;
	private Label MST1;
	private Label MSR2;
	private Label MST2;
	
	private TextArea messageAreaIn;

	private ConfigurationPanel configurationPanel;
	private SerialParameters parameters;
	private SerialConnection connection;
	
	Font myFont = new Font("Serif", Font.PLAIN, 22);
	private Properties props = null;
	int stoptime;
	/**
	 * Main method. Checks to see if the command line agrument is requesting
	 * usage informaition (-h, -help), if it is, display a usage message and
	 * exit, otherwise create a new <code>SerialDemo</code> and set it visible.
	 */
	public static void main(String[] args) {

		
		LibraryUtility.libraryInstall();
		if ((args.length > 0)
				&& (args[0].equals("-h") || args[0].equals("-help"))) {
			System.out.println("usage: java SerialDemo [configuration File]");
			System.exit(1);
		}

		SerialDemo serialDemo = new SerialDemo(args);
		serialDemo.setVisible(true);
		serialDemo.repaint();

	}

	/**
	 * Create new <code>SerialDemo</code> and initilizes it. Parses args to find
	 * configuration file. If found, initial state it set to parameters in
	 * configuration file.
	 * 
	 * @param args
	 *            command line arguments used when program was invoked.
	 */
	public SerialDemo(String[] args) {
		super("Hardness UART");

		parameters = new SerialParameters();

		// Set up the GUI for the program
		addWindowListener(new CloseHandler(this));

		mb = new MenuBar();
		mb.setFont(myFont);
		fileMenu = new Menu("File");

		openItem = new MenuItem("Load");
		openItem.addActionListener(this);
		fileMenu.add(openItem);

		saveItem = new MenuItem("Save");
		saveItem.addActionListener(this);
		fileMenu.add(saveItem);

		exitItem = new MenuItem("Exit");
		exitItem.addActionListener(this);
		fileMenu.add(exitItem);

		mb.add(fileMenu);

		setMenuBar(mb);

		messagePanel = new Panel();
		messagePanel.setLayout(new GridLayout(9, 1));


		MSR1 = new Label("Initial Speed(RPM):", Label.LEFT);
		messagePanel.add(MSR1);
		MSR1.setFont(myFont);
		
		messageAreaOut = new TextArea();
		messagePanel.add(messageAreaOut);
		
		MST1 = new Label("Initial Speed time (min):", Label.LEFT);
		messagePanel.add(MST1);
		MST1.setFont(myFont);
		
		messageAreaOutT1 = new TextArea();
		messagePanel.add(messageAreaOutT1);
		
		MSR2 = new Label("Final Speed(RPM):", Label.LEFT);
		messagePanel.add(MSR2);
		MSR2.setFont(myFont);
		
		messageAreaOutR2 = new TextArea();
		messagePanel.add(messageAreaOutR2);
		
		MST2 = new Label("Final Speed time (min):", Label.LEFT);
		messagePanel.add(MST2);
		MST2.setFont(myFont);
		
		messageAreaOutT2 = new TextArea();
		messagePanel.add(messageAreaOutT2);
		

		messageAreaOut.setFont(myFont);
		messageAreaOutT1.setFont(myFont);
		messageAreaOutR2.setFont(myFont);
		messageAreaOutT2.setFont(myFont);

		
		messageAreaIn = new TextArea();
		messageAreaIn.setEditable(false);
		messagePanel.add(messageAreaIn);

		messageAreaIn.setFont(myFont);
		add(messagePanel, "Center");

		graphicPanel = new Panel();

		configurationPanel = new ConfigurationPanel(this);

		buttonPanel = new Panel();

		openButton = new Button("Open Port");
		openButton.addActionListener(this);
		openButton.setFont(myFont);
		buttonPanel.add(openButton);

		closeButton = new Button("Close Port");
		closeButton.setFont(myFont);
		closeButton.addActionListener(this);
		closeButton.setEnabled(false);
		buttonPanel.add(closeButton);

		breakButton = new Button("Send Cmd");
		breakButton.setFont(myFont);

		breakButton.addActionListener(this);
		buttonPanel.add(breakButton);

//		plotButton = new Button("Start");
//		plotButton.addActionListener(this);
//		plotButton.setEnabled(false);
//		buttonPanel.add(plotButton);
//		
//		StopButton = new Button("Stop");
//		StopButton.addActionListener(this);
//		StopButton.setEnabled(false);
//		buttonPanel.add(StopButton);

		Panel southPanel = new Panel();

		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints cons = new GridBagConstraints();

		southPanel.setLayout(gridBag);

		cons.gridwidth = GridBagConstraints.REMAINDER;
		gridBag.setConstraints(configurationPanel, cons);
		cons.weightx = 1.0;
		southPanel.add(configurationPanel);
		gridBag.setConstraints(buttonPanel, cons);
		southPanel.add(buttonPanel);

		add(southPanel, "South");

		parseArgs(args);

		connection = new SerialConnection(this, parameters, messageAreaOut,
				messageAreaOutR2,
			    messageAreaOutT1,
			    messageAreaOutT2,
				messageAreaIn);
		setConfigurationPanel();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		setLocation(screenSize.width / 2 - WIDTH / 2, screenSize.height / 2
				- HEIGHT / 2);

		setSize(WIDTH, HEIGHT);
	}

	/**
	 * Sets the GUI elements on the configurationPanel.
	 */
	public void setConfigurationPanel() {
		configurationPanel.setConfigurationPanel();
	}

	/**
	 * Responds to the menu items and buttons.
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		// Loads a configuration file.
		if (cmd.equals("Load")) {
			if (connection.isOpen()) {
				AlertDialog ad = new AlertDialog(this, "Port Open!",
						"Configuration may not", "be loaded",
						"while a port is open.");
			} else {
				FileDialog fd = new FileDialog(this, "Load Port Configuration",
						FileDialog.LOAD);
				fd.setVisible(true);
				String file = fd.getFile();
				if (file != null) {
					String dir = fd.getDirectory();
					File f = new File(dir + file);
					try {
						FileInputStream fis = new FileInputStream(f);
						props = new Properties();
						props.load(fis);
						fis.close();
					} catch (FileNotFoundException e1) {
						System.err.println(e1);
					} catch (IOException e2) {
						System.err.println(e2);
					}
					loadParams();
				}
			}
		}

		// Saves a configuration file.
		if (cmd.equals("Save")) {
			configurationPanel.setParameters();
			FileDialog fd = new FileDialog(this, "Save Port Configuration",
					FileDialog.SAVE);
			fd.setFile("serialdemo.properties");
			fd.setVisible(true);
			String fileName = fd.getFile();
			String directory = fd.getDirectory();
			if ((fileName != null) && (directory != null)) {
				writeFile(directory + fileName);
			}
		}

		// Calls shutdown, which exits the program.
		if (cmd.equals("Exit")) {
			shutdown();
		}

		// Opens a port.
		if (cmd.equals("Open Port")) {
			openButton.setEnabled(false);
			Cursor previousCursor = getCursor();
			setNewCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			configurationPanel.setParameters();
			try {
				connection.openConnection();
			} catch (SerialConnectionException e2) {
				AlertDialog ad = new AlertDialog(this, "Error Opening Port!",
						"Error opening port,", e2.getMessage() + ".",
						"Select new settings, try again.");
				openButton.setEnabled(true);
				setNewCursor(previousCursor);
				return;
			}
			portOpened();
			setNewCursor(previousCursor);
		}

		// Closes a port.
		if (cmd.equals("Close Port")) {
			portClosed();
		}

		// Sends a break signal to the port.
		if (cmd.equals("Send Cmd")) {
			connection.sendBreak();

			//plotButton.setEnabled(true);
			//breakButton.setEnabled(false);
			messageAreaOut.setText("*");
			messageAreaOut.setText("");

			messageAreaOutT1.setText("*");
			messageAreaOutT1.setText("");

			messageAreaOutR2.setText("*");
			messageAreaOutR2.setText("");

			messageAreaOutT2.setText("*");
			messageAreaOutT2.setText("");
			
		}
		if (cmd.equals("Start")) {
			// TimeSeriesChartDemo1 demo = new TimeSeriesChartDemo1(
			// "Force-Displacement Chart");
			// demo.pack();
			// RefineryUtilities.centerFrameOnScreen(demo);
			// demo.setVisible(true);
				connection.sendStart();
				//plotButton.setEnabled(false);
				StopButton.setEnabled(true);
		}
		if (cmd.equals("Stop")) {
				connection.sendStop();
				StopButton.setEnabled(false);
				//breakButton.setEnabled(true);
		}
	}

	/**
	 * Toggles the buttons to an open port state.
	 */
	public void portOpened() {
		openButton.setEnabled(false);
		closeButton.setEnabled(true);
		breakButton.setEnabled(true);
//		StopButton.setEnabled(false);
		messageAreaIn.setText("*");
		messageAreaIn.setText("");
	}

	/**
	 * Calls closeConnection on the SerialConnection and toggles the buttons to
	 * a closed port state.
	 */
	public void portClosed() {
		connection.closeConnection();
		openButton.setEnabled(true);
		closeButton.setEnabled(false);
		breakButton.setEnabled(false);
		//plotButton.setEnabled(false);
	}

	/**
	 * Sets the <code>Cursor</code> for the application.
	 * 
	 * @param c
	 *            New <code>Cursor</code>
	 */
	private void setNewCursor(Cursor c) {
		setCursor(c);
		messageAreaIn.setCursor(c);
		messageAreaIn.toString();
		messageAreaOut.setCursor(c);
	}

	/**
	 * Writes the current parameters to a configuration file of the
	 * java.properties style.
	 */
	private void writeFile(String path) {

		Properties newProps;
		FileOutputStream fileOut = null;

		newProps = new Properties();

		newProps.put("portName", parameters.getPortName());
		newProps.put("baudRate", parameters.getBaudRateString());
		newProps.put("flowControlIn", parameters.getFlowControlInString());
		newProps.put("flowControlOut", parameters.getFlowControlOutString());
		newProps.put("parity", parameters.getParityString());
		newProps.put("databits", parameters.getDatabitsString());
		newProps.put("stopbits", parameters.getStopbitsString());
		newProps.put("Running(min.)", parameters.getStoptime());
		newProps.put("ID", parameters.getStationID());

		try {
			fileOut = new FileOutputStream(path);
		} catch (IOException e) {
			System.out.println("Could not open file for writiing");
		}

		newProps.save(fileOut, "Serial Demo poperties");

		try {
			fileOut.close();
		} catch (IOException e) {
			System.out.println("Could not close file for writiing");
		}
	}

	/**
	 * Cleanly shuts down the applicaion. first closes any open ports and cleans
	 * up, then exits.
	 */
	private void shutdown() {
		connection.closeConnection();
		System.exit(1);
	}

	/**
	 * Finds configuration file in arguments and creates a properties object
	 * from that file.
	 */
	private void parseArgs(String[] args) {
		if (args.length < 1) {
			return;
		}

		File f = new File(args[0]);

		if (!f.exists()) {
			f = new File(System.getProperty("user.dir")
					+ System.getProperty("path.separator") + args[0]);
		}

		if (f.exists()) {
			try {
				FileInputStream fis = new FileInputStream(f);
				props = new Properties();
				props.load(fis);
				fis.close();
				loadParams();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Set the parameters object to the settings in the properties object.
	 */
	private void loadParams() {
		parameters.setPortName(props.getProperty("portName"));
		parameters.setBaudRate(props.getProperty("baudRate"));
		parameters.setFlowControlIn(props.getProperty("flowControlIn"));
		parameters.setFlowControlOut(props.getProperty("flowControlOut"));
		parameters.setParity(props.getProperty("parity"));
		parameters.setDatabits(props.getProperty("databits"));
		parameters.setStopbits(props.getProperty("stopbits"));
		parameters.setStoptimeString(props.getProperty("stoptime"));

		parameters.setFunctionCode(props.getProperty("functionCode"));

		setConfigurationPanel();
	}

	/**
	 * GUI element that holds the user changable elements for connection
	 * configuration.
	 */
	class ConfigurationPanel extends Panel implements ItemListener {

		private Frame parent;

		private Label portNameLabel;
		private Choice portChoice;

		private Label baudLabel;
		private Choice baudChoice;

		private Label flowControlInLabel;
		private Choice flowChoiceIn;

		private Label flowControlOutLabel;
		private Choice flowChoiceOut;

		private Label databitsLabel;
		private Choice databitsChoice;

		private Label stopbitsLabel;
		private Choice stopbitsChoice;
		
		private Label stoptimeLabel;
		private Choice stoptimeChoice;
		
		private Label stationIDLabel;
		private Choice stationIDChoice;
		
		private Label functionCodeLabel;
		private Choice functionCodeChoice;
		
		private Label parityLabel;
		private Choice parityChoice;

		/**
		 * Creates and initilizes the configuration panel. The initial settings
		 * are from the parameters object.
		 */
		public ConfigurationPanel(Frame parent) {
			this.parent = parent;

			setLayout(new GridLayout(5, 4));

			portNameLabel = new Label("Port Name:", Label.LEFT);
			add(portNameLabel);
			portNameLabel.setFont(myFont);
			
			portChoice = new Choice();
			portChoice.setFont(myFont);
			portChoice.addItemListener(this);
			add(portChoice);
			listPortChoices();
			portChoice.select(parameters.getPortName());

			baudLabel = new Label("Baud Rate:", Label.LEFT);
			baudLabel.setFont(myFont);
			add(baudLabel);

			baudChoice = new Choice();
			baudChoice.setFont(myFont);
			baudChoice.addItem("300");
			baudChoice.addItem("2400");
			baudChoice.addItem("9600");
			baudChoice.addItem("14400");
			baudChoice.addItem("28800");
			baudChoice.addItem("38400");
			baudChoice.addItem("57600");
			baudChoice.addItem("115200");
			baudChoice.select(Integer.toString(parameters.getBaudRate()));
			baudChoice.addItemListener(this);
			add(baudChoice);

			flowControlInLabel = new Label("Flow Control In:", Label.LEFT);
			flowControlInLabel.setFont(myFont);
			add(flowControlInLabel);

			flowChoiceIn = new Choice();
			flowChoiceIn.setFont(myFont);
			flowChoiceIn.addItem("None");
			flowChoiceIn.addItem("Xon/Xoff In");
			flowChoiceIn.addItem("RTS/CTS In");
			flowChoiceIn.select(parameters.getFlowControlInString());
			flowChoiceIn.addItemListener(this);
			add(flowChoiceIn);

			flowControlOutLabel = new Label("Flow Control Out:", Label.LEFT);
			flowControlOutLabel.setFont(myFont);
			add(flowControlOutLabel);

			flowChoiceOut = new Choice();
			flowChoiceOut.setFont(myFont);
			flowChoiceOut.addItem("None");
			flowChoiceOut.addItem("Xon/Xoff Out");
			flowChoiceOut.addItem("RTS/CTS Out");
			flowChoiceOut.select(parameters.getFlowControlOutString());
			flowChoiceOut.addItemListener(this);
			add(flowChoiceOut);

			databitsLabel = new Label("Data Bits:", Label.LEFT);
			databitsLabel.setFont(myFont);
			add(databitsLabel);

			databitsChoice = new Choice();
			databitsChoice.setFont(myFont);
			databitsChoice.addItem("5");
			databitsChoice.addItem("6");
			databitsChoice.addItem("7");
			databitsChoice.addItem("8");
			databitsChoice.select(parameters.getDatabitsString());
			databitsChoice.addItemListener(this);
			add(databitsChoice);

			stopbitsLabel = new Label("Stop Bits:", Label.LEFT);
			stopbitsLabel.setFont(myFont);
			add(stopbitsLabel);

			stopbitsChoice = new Choice();
			stopbitsChoice.setFont(myFont);
			stopbitsChoice.addItem("1");
			stopbitsChoice.addItem("1.5");
			stopbitsChoice.addItem("2");
			stopbitsChoice.select(parameters.getStopbitsString());
			stopbitsChoice.addItemListener(this);
			add(stopbitsChoice);
			//Set speed
			stoptimeLabel = new Label("Running(min.):", Label.LEFT);
			stoptimeLabel.setFont(myFont);
			add(stoptimeLabel);

			stoptimeChoice = new Choice();
			stoptimeChoice.setFont(myFont);
			stoptimeChoice.addItem("1");
			stoptimeChoice.addItem("2");
			stoptimeChoice.addItem("3");
			stoptimeChoice.addItem("40");
			stoptimeChoice.select(parameters.getStopbitsString());
			stoptimeChoice.addItemListener(this);
			add(stoptimeChoice);
	
			parityLabel = new Label("Parity:", Label.LEFT);
			parityLabel.setFont(myFont);
			add(parityLabel);

			parityChoice = new Choice();
			parityChoice.setFont(myFont);
			parityChoice.addItem("None");
			parityChoice.addItem("Even");
			parityChoice.addItem("Odd");
			parityChoice.select("None");
			parityChoice.select(parameters.getParityString());
			parityChoice.addItemListener(this);
			add(parityChoice);
			
			
			stationIDLabel = new Label("Station ID:", Label.LEFT);
			stationIDLabel.setFont(myFont);
			add(stationIDLabel);

			stationIDChoice = new Choice();
			stationIDChoice.setFont(myFont);
			stationIDChoice.setFont(myFont);
			stationIDChoice.addItem("1");
			stationIDChoice.addItem("2");
			stationIDChoice.addItem("3");
			stationIDChoice.select(parameters.getStationID());
			stationIDChoice.addItemListener(this);
			add(stationIDChoice);
			
			functionCodeLabel = new Label("Function code:", Label.LEFT);
			functionCodeLabel.setFont(myFont);
			add(functionCodeLabel);

			functionCodeChoice = new Choice();
			functionCodeChoice.setFont(myFont);
			functionCodeChoice.addItem("3");
			functionCodeChoice.addItem("6");
			functionCodeChoice.addItem("10");
			functionCodeChoice.select(parameters.getFunctionCode());
			functionCodeChoice.addItemListener(this);
			add(functionCodeChoice);
			
		}

		/**
		 * Sets the configuration panel to the settings in the parameters
		 * object.
		 */
		public void setConfigurationPanel() {
			portChoice.select(parameters.getPortName());
			baudChoice.select(parameters.getBaudRateString());
			flowChoiceIn.select(parameters.getFlowControlInString());
			flowChoiceOut.select(parameters.getFlowControlOutString());
			databitsChoice.select(parameters.getDatabitsString());
			stopbitsChoice.select(parameters.getStopbitsString());
			stoptimeChoice.select(parameters.getStoptime());
			stationIDChoice.select(parameters.getStationID());
			functionCodeChoice.select(parameters.getFunctionCode());
			parityChoice.select(parameters.getParityString());
		}

		/**
		 * Sets the parameters object to the settings in the configuration
		 * panel.
		 */
		public void setParameters() {
			parameters.setPortName(portChoice.getSelectedItem());
			parameters.setBaudRate(baudChoice.getSelectedItem());
			parameters.setFlowControlIn(flowChoiceIn.getSelectedItem());
			parameters.setFlowControlOut(flowChoiceOut.getSelectedItem());
			parameters.setDatabits(databitsChoice.getSelectedItem());
			parameters.setStopbits(stopbitsChoice.getSelectedItem());
			parameters.setStoptimeString(stoptimeChoice.getSelectedItem());
			parameters.setStationIDString(stationIDChoice.getSelectedItem());

			parameters.setFunctionCode(functionCodeChoice.getSelectedItem());
			parameters.setParity(parityChoice.getSelectedItem());
		}

		/**
		 * Sets the elements for the portChoice from the ports available on the
		 * system. Uses an emuneration of comm ports returned by
		 * CommPortIdentifier.getPortIdentifiers(), then sets the current choice
		 * to a mathing element in the parameters object.
		 */
		void listPortChoices() {
			CommPortIdentifier portId;

			Enumeration en = CommPortIdentifier.getPortIdentifiers();

			// iterate through the ports.
			while (en.hasMoreElements()) {
				portId = (CommPortIdentifier) en.nextElement();
				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					portChoice.addItem(portId.getName());
				}
			}
			portChoice.select(parameters.getPortName());
		}

		/**
		 * Event handler for changes in the current selection of the Choices. If
		 * a port is open the port can not be changed. If the choice is
		 * unsupported on the platform then the user will be notified and the
		 * settings will revert to their pre-selection state.
		 */
		public void itemStateChanged(ItemEvent e) {
			// Check if port is open.
			if (connection.isOpen()) {
				// If port is open do not allow port to change.
				if (e.getItemSelectable() == portChoice) {
					// Alert user.
					AlertDialog ad = new AlertDialog(parent, "Port Open!",
							"Port can not", "be changed",
							"while a port is open.");

					// Return configurationPanel to pre-choice settings.
					setConfigurationPanel();
					return;
				}
				// Set the parameters from the choice panel.
				setParameters();
				try {
					// Attempt to change the settings on an open port.
					connection.setConnectionParameters();
				} catch (SerialConnectionException ex) {
					// If setting can not be changed, alert user, return to
					// pre-choice settings.
					AlertDialog ad = new AlertDialog(parent,
							"Unsupported Configuration!",
							"Configuration Parameter unsupported,",
							"select new value.",
							"Returning to previous configuration.");
					setConfigurationPanel();
				}
			} else {
				// Since port is not open just set the parameter object.
				setParameters();
			}
		}
	}

	/**
	 * Handles closing down system. Allows application to be closed with window
	 * close box.
	 */
	class CloseHandler extends WindowAdapter {

		SerialDemo sd;

		public CloseHandler(SerialDemo sd) {
			this.sd = sd;
		}

		public void windowClosing(WindowEvent e) {
			sd.shutdown();
		}
	}
}
