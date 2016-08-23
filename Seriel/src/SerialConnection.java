/* @(#)SerialConnection.java	1.6 98/07/17 SMI
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
import java.nio.ByteBuffer;
import java.awt.TextArea;
import java.awt.event.*;
import java.util.TooManyListenersException;

/**
A class that handles the details of a serial connection. Reads from one 
TextArea and writes to a second TextArea. 
Holds the state of the connection.
*/
public class SerialConnection implements SerialPortEventListener, 
					 CommPortOwnershipListener {
    private SerialDemo parent;

    private TextArea messageAreaOut;
    
    private TextArea messageAreaOutT1;
    private TextArea messageAreaOutR2;
    private TextArea messageAreaOutT2;
    
    private TextArea messageAreaIn;
    private SerialParameters parameters;
    private OutputStream os;
    private InputStream is;
    private KeyHandler keyHandler;

    private CommPortIdentifier portId;
	private SerialPort sPort;

    private boolean open;
    static String messageString;

    static String messageStringT1;

    static String messageStringT2;

    static String messageStringR2;
    /**
    Creates a SerialConnection object and initilizes variables passed in
    as params.

    @param parent A SerialDemo object.
    @param parameters A SerialParameters object.
    @param messageAreaOut The TextArea that messages that are to be sent out
    of the serial port are entered into.
    @param messageAreaIn The TextArea that messages comming into the serial
    port are displayed on.
    */
    public SerialConnection(SerialDemo parent,
			    SerialParameters parameters,
			    TextArea messageAreaOut,
			    TextArea messageAreaOutR2,
			    TextArea messageAreaOutT1,
			    TextArea messageAreaOutT2,
			    TextArea messageAreaIn) {
	this.parent = parent;
	this.parameters = parameters;
	this.messageAreaOut = messageAreaOut;
	this.messageAreaOutR2 = messageAreaOutR2;
	this.messageAreaOutT1 = messageAreaOutT1;
	this.messageAreaOutT2 = messageAreaOutT2;
	this.messageAreaIn = messageAreaIn;
	open = false;
   }

   /**
   Attempts to open a serial connection and streams using the parameters
   in the SerialParameters object. If it is unsuccesfull at any step it
   returns the port to a closed state, throws a 
   <code>SerialConnectionException</code>, and returns.

   Gives a timeout of 30 seconds on the portOpen to allow other applications
   to reliquish the port if have it open and no longer need it.
   */
   public void openConnection() throws SerialConnectionException {

	// Obtain a CommPortIdentifier object for the port you want to open.
	try {
	    portId = 
		 CommPortIdentifier.getPortIdentifier(parameters.getPortName());
	} catch (NoSuchPortException e) {
	    throw new SerialConnectionException(e.getMessage());
	}

	// Open the port represented by the CommPortIdentifier object. Give
	// the open call a relatively long timeout of 30 seconds to allow
	// a different application to reliquish the port if the user 
	// wants to.
	try {
	    sPort = (SerialPort)portId.open("SerialDemo", 300000);
	} catch (PortInUseException e) {
	    throw new SerialConnectionException(e.getMessage());
	}

	// Set the parameters of the connection. If they won't set, close the
	// port before throwing an exception.
	try {
	    setConnectionParameters();
	} catch (SerialConnectionException e) {	
	    sPort.close();
	    throw e;
	}

	// Open the input and output streams for the connection. If they won't
	// open, close the port before throwing an exception.
	try {
	    os = sPort.getOutputStream();
	    is = sPort.getInputStream();
	} catch (IOException e) {
	    sPort.close();
	    throw new SerialConnectionException("Error opening i/o streams");
	}

	// Create a new KeyHandler to respond to key strokes in the 
	// messageAreaOut. Add the KeyHandler as a keyListener to the 
	// messageAreaOut.
	keyHandler = new KeyHandler(os);
	////messageAreaOut.addKeyListener(keyHandler);

	// Add this object as an event listener for the serial port.
	try {
	    sPort.addEventListener(this);
	} catch (TooManyListenersException e) {
	    sPort.close();
	    throw new SerialConnectionException("too many listeners added");
	}

	// Set notifyOnDataAvailable to true to allow event driven input.
	sPort.notifyOnDataAvailable(true);

	// Set notifyOnBreakInterrup to allow event driven break handling.
	sPort.notifyOnBreakInterrupt(true);

	// Set receive timeout to allow breaking out of polling loop during
	// input handling.
	try {
	    sPort.enableReceiveTimeout(30);
	} catch (UnsupportedCommOperationException e) {
	}

	// Add ownership listener to allow ownership event handling.
	portId.addPortOwnershipListener(this);

	open = true;
    }

    /**
    Sets the connection parameters to the setting in the parameters object.
    If set fails return the parameters object to origional settings and
    throw exception.
    */
    public void setConnectionParameters() throws SerialConnectionException {

	// Save state of parameters before trying a set.
	int oldBaudRate = sPort.getBaudRate();
	int oldDatabits = sPort.getDataBits();
	int oldStopbits = sPort.getStopBits();
	int oldParity   = sPort.getParity();
	int oldFlowControl = sPort.getFlowControlMode();

	// Set connection parameters, if set fails return parameters object
	// to original state.
	try {
	    sPort.setSerialPortParams(parameters.getBaudRate(),
				      parameters.getDatabits(),
				      parameters.getStopbits(),
				      parameters.getParity());
	} catch (UnsupportedCommOperationException e) {
	    parameters.setBaudRate(oldBaudRate);
	    parameters.setDatabits(oldDatabits);
	    parameters.setStopbits(oldStopbits);
	    parameters.setParity(oldParity);
	    throw new SerialConnectionException("Unsupported parameter");
	}

	// Set flow control.
	try {
	    sPort.setFlowControlMode(parameters.getFlowControlIn() 
			           | parameters.getFlowControlOut());
	} catch (UnsupportedCommOperationException e) {
	    throw new SerialConnectionException("Unsupported flow control");
	}
    }

    /**
    Close the port and clean up associated elements.
    */
    public void closeConnection() {
	// If port is alread closed just return.
	if (!open) {
	    return;
	}

	// Remove the key listener.
	messageAreaOut.removeKeyListener(keyHandler);

	// Check to make sure sPort has reference to avoid a NPE.
	if (sPort != null) {
	    try {
		// close the i/o streams.
	    	os.close();
	    	is.close();
	    } catch (IOException e) {
		System.err.println(e);
	    }

	    // Close the port.
	    sPort.close();

	    // Remove the ownership listener.
	    portId.removePortOwnershipListener(this);
	}

	open = false;
    }

    /**
    Send a one second break signal.
    */
    public void sendBreak() {
    	//sPort.sendBreak(1000);
        	
            try {
    			messageAreaIn.setText("RPM :　");
            	messageString=messageAreaOut.getText();
            	messageString.replaceAll(" ", "");        	//刪除空格
            	
            	if(!messageString.equals("")){
            	os.write(this.parameters.getStationID());
            	os.write(this.parameters.getFunctionCode());
            	os.write(00);
            	os.write(00);
            	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode()+" 0x00"+" 0x00");
    		    for(int i =0;i<messageString.length();i=i+2){//取兩格字元元素為一組
    		    	os.write(Integer.parseInt(messageString.charAt(i)+""+messageString.charAt(i+1),16));
    			    System.out.print(" 0x"+messageString.charAt(i)+""+messageString.charAt(i+1));
    		    }
//    		    chksum[4]=(byte) Integer.parseInt(messageString.charAt(0)+""+messageString.charAt(1),16);
//    		    chksum[5]=(byte) Integer.parseInt(messageString.charAt(2)+""+messageString.charAt(3),16);
    		    //check sum

            	ModbusCRC16 crc = new ModbusCRC16();
    	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),00,00,Integer.parseInt(messageString.charAt(0)+""+messageString.charAt(1),16),Integer.parseInt(messageString.charAt(2)+""+messageString.charAt(3),16)};
    	        for (int d : data) {
    	            crc.update(d);
    	        }
    	        byte[] byteStr = new byte[2];
    	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
    	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
            	os.write(byteStr[0]);
            	os.write(byteStr[1]);
            	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
    		    	System.out.println("");

            	}
            	
    	        	messageString=messageAreaOutT1.getText();
    	        	messageString.replaceAll(" ", "");        	//刪除空格
    	        	if(!messageString.equals("")){
    	        	os.write(this.parameters.getStationID());
    	        	os.write(this.parameters.getFunctionCode());
    	        	os.write(00);
    	        	os.write(01);
    	        	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode()+" 0x00"+" 0x01");
    			    for(int i =0;i<messageString.length();i=i+2){//取兩格字元元素為一組
    			    	os.write(Integer.parseInt(messageString.charAt(i)+""+messageString.charAt(i+1),16));
    				    System.out.print(" 0x"+messageString.charAt(i)+""+messageString.charAt(i+1));
    			    }
					  //check sum
                	ModbusCRC16 crc = new ModbusCRC16();
	    	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),00,01,Integer.parseInt(messageString.charAt(0)+""+messageString.charAt(1),16),Integer.parseInt(messageString.charAt(2)+""+messageString.charAt(3),16)};
	    	        for (int d : data) {
	    	            crc.update(d);
	    	        }
	    	        byte[] byteStr = new byte[2];
	    	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
	    	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
	            	os.write(byteStr[0]);
	            	os.write(byteStr[1]);
	            	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
    			    	System.out.println("");
    	        	}
    			    	
    		        	messageString=messageAreaOutR2.getText();
    		        	messageString.replaceAll(" ", "");        	//刪除空格
    		        	if(!messageString.equals("")){
    		        	os.write(this.parameters.getStationID());
    		        	os.write(this.parameters.getFunctionCode());
    		        	os.write(00);
    		        	os.write(02);
    		        	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode()+" 0x00"+" 0x02");
    				    for(int i =0;i<messageString.length();i=i+2){//取兩格字元元素為一組
    				    	os.write(Integer.parseInt(messageString.charAt(i)+""+messageString.charAt(i+1),16));
    					    System.out.print(" 0x"+messageString.charAt(i)+""+messageString.charAt(i+1));
    				    }
    				    //check sum

    	            	ModbusCRC16 crc = new ModbusCRC16();
		    	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),00,02,Integer.parseInt(messageString.charAt(0)+""+messageString.charAt(1),16),Integer.parseInt(messageString.charAt(2)+""+messageString.charAt(3),16)};
		    	        for (int d : data) {
		    	            crc.update(d);
		    	        }
		    	        byte[] byteStr = new byte[2];
		    	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
		    	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
		            	os.write(byteStr[0]);
		            	os.write(byteStr[1]);
		            	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
    				    	System.out.println("");
    		        	}
    		        	

			        	messageString=messageAreaOutT2.getText();
			        	messageString.replaceAll(" ", "");        	//刪除空格
    		        	
    		        	if(!messageString.equals("")){
    			        	os.write(this.parameters.getStationID());
    			        	os.write(this.parameters.getFunctionCode());
    			        	os.write(00);
    			        	os.write(03);
        		        	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode()+" 0x00"+" 0x02");
        				    for(int i =0;i<messageString.length();i=i+2){//取兩格字元元素為一組
        				    	os.write(Integer.parseInt(messageString.charAt(i)+""+messageString.charAt(i+1),16));
        					    System.out.print(" 0x"+messageString.charAt(i)+""+messageString.charAt(i+1));
        				    }
        				    //check sum

        	            	ModbusCRC16 crc = new ModbusCRC16();
    		    	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),00,03,Integer.parseInt(messageString.charAt(0)+""+messageString.charAt(1),16),Integer.parseInt(messageString.charAt(2)+""+messageString.charAt(3),16)};
    		    	        for (int d : data) {
    		    	            crc.update(d);
    		    	        }
    		    	        byte[] byteStr = new byte[2];
    		    	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
    		    	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
    		            	os.write(byteStr[0]);
    		            	os.write(byteStr[1]);
    		            	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
        				    	System.out.println("");
    		        	}
            	os.flush();
            } catch (IOException e4) {}
        }
    
    public void sendtime(int key,String time,String speed) {
        try {
        	//receive android list data
        	//insert 0
        	String Tkey;

        	Tkey=Integer.toHexString(key+20).toString();
        	if((""+Tkey).length()==0){
        		Tkey="0000"+Tkey;
        	}        	
        	else if((""+Tkey).length()==1){
        		Tkey="000"+Tkey;
        	}
        	else if((""+Tkey).length()==2){
        		Tkey="00"+Tkey;
        	}
        	else if((""+Tkey).length()==3){
        		Tkey="0"+Tkey;
        	}
        	else
        	{
        		Tkey=""+Tkey;
        	}
        	time=Integer.toHexString(Integer.parseInt(time)).toString();

        	if(time.length()==0){
        		time="0000"+time;   
        		time=""+Integer.toHexString(0000);
        	}        	
        	else if(time.length()==1){
        		time="000"+time;   
        	}
        	else if(time.length()==2){
        		time="00"+time;
        	}
        	else if(time.length()==3){
        		time="0"+time;
        	}
        	else
        	{
        		time=""+time;
        	}        	
        	
        	//send time to register from add 0x20
	        	os.write(this.parameters.getStationID());
	        	os.write(this.parameters.getFunctionCode());

	        	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode());
			    
	        	for(int i =0;i<Tkey.length();i=i+2){//取兩格字元元素為一組
			    	os.write(Integer.parseInt(Tkey.charAt(i)+""+Tkey.charAt(i+1),16));
				    System.out.print(" 0x"+Tkey.charAt(i)+""+Tkey.charAt(i+1));
			    }
	        	
	        	for(int i =0;i<time.length();i=i+2){//取兩格字元元素為一組
			    	os.write(Integer.parseInt(time.charAt(i)+""+time.charAt(i+1),16));
				    System.out.print(" 0x"+time.charAt(i)+""+time.charAt(i+1));
			    }
			    //check sum

            	ModbusCRC16 crc = new ModbusCRC16();
    	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),Integer.parseInt(Tkey.charAt(0)+""+Tkey.charAt(1),16),Integer.parseInt(Tkey.charAt(2)+""+Tkey.charAt(3),16),Integer.parseInt(time.charAt(0)+""+time.charAt(1),16),Integer.parseInt(time.charAt(2)+""+time.charAt(3),16)};
    	        for (int d : data) {
    	            crc.update(d);
    	        }
    	        byte[] byteStr = new byte[2];
    	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
    	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
            	os.write(byteStr[0]);
            	os.write(byteStr[1]);
            	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
			    	System.out.println("");
			   os.flush();
        } catch (IOException e4) {
        	System.out.println("send to uart error in send speed");
        }
    }
        
        public void sendspeed(int key,String time,String speed) {
            try {
            	//receive android list data
            	//insert 0
            	String Skey;

            	Skey=Integer.toHexString(key+10).toString();
            	if((""+Skey).length()==0){
            		Skey="0000"+Skey;
            	}        	
            	else if((""+Skey).length()==1){
            		Skey="000"+Skey;
            	}
            	else if((""+Skey).length()==2){
            		Skey="00"+Skey;
            	}
            	else if((""+Skey).length()==3){
            		Skey="0"+Skey;
            	}
            	else
            	{
            		Skey=""+Skey;
            	}

            	speed=Integer.toHexString(Integer.parseInt(speed)).toString();
            	if(speed.length()==0){
            		speed="0000"+speed;
            	}        	
            	else if(speed.length()==1){
            		speed="000"+speed;
            	}
            	else if(speed.length()==2){
            		speed="00"+speed;
            	}
            	else if(speed.length()==3){
            		speed="0"+speed;
            	}
            	else
            	{
            		speed=""+speed;
            	}        	
            	
            	//send time to register from add 0x20
    	        	os.write(this.parameters.getStationID());
    	        	os.write(this.parameters.getFunctionCode());

    	        	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode());
    			    
    	        	for(int i =0;i<Skey.length();i=i+2){//取兩格字元元素為一組
    			    	os.write(Integer.parseInt(Skey.charAt(i)+""+Skey.charAt(i+1),16));
    				    System.out.print(" 0x"+Skey.charAt(i)+""+Skey.charAt(i+1));
    			    }
    	        	
    	        	for(int i =0;i<speed.length();i=i+2){//取兩格字元元素為一組
    			    	os.write(Integer.parseInt(speed.charAt(i)+""+speed.charAt(i+1),16));
    				    System.out.print(" 0x"+speed.charAt(i)+""+speed.charAt(i+1));
    			    }
    			    //check sum

                	ModbusCRC16 crc = new ModbusCRC16();
        	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),Integer.parseInt(Skey.charAt(0)+""+Skey.charAt(1),16),Integer.parseInt(Skey.charAt(2)+""+Skey.charAt(3),16),Integer.parseInt(speed.charAt(0)+""+speed.charAt(1),16),Integer.parseInt(speed.charAt(2)+""+speed.charAt(3),16)};
        	        for (int d : data) {
        	            crc.update(d);
        	        }
        	        byte[] byteStr = new byte[2];
        	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
        	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
                	os.write(byteStr[0]);
                	os.write(byteStr[1]);
                	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
    			    	System.out.println("");
    			   os.flush();
            } catch (IOException e4) {
            	System.out.println("send to uart error in send speed");
            }
    }
    
    
    
    
    
    public void sendStart(){
        try {
//        	messageString="*";
//
//        	os.write(messageString.getBytes());
        	//os.write(Integer.parseInt(messageString));
        	
        	//sent start state to register add 0x05 value 0x01
        	os.write(this.parameters.getStationID());
        	os.write(this.parameters.getFunctionCode());
        	os.write(00);
        	os.write(01);
        	os.write(00);
        	os.write(01);
        	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode()+" 0x00"+" 0x01"+" 0x00"+" 0x01");

			  //check sum
        	ModbusCRC16 crc = new ModbusCRC16();
	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),00,01,00,01};
	        for (int d : data) {
	            crc.update(d);
	        }
	        byte[] byteStr = new byte[2];
	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
        	os.write(byteStr[0]);
        	os.write(byteStr[1]);
        	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
		    	System.out.println("");
        	
        	os.flush();
//        	System.out.println(messageString.toString());
        } catch (IOException e5) {
        	System.out.println("Send uart error in sendstart()");
        	System.out.println(e5);
        }
    }
    
    public void sendZero(){
        try {
        	String crcadd;
        	//send zero
        	//sent start state to register add 0x05 value 0x01
        	for(int i=10;i<31;i++){
        	os.write(this.parameters.getStationID());
        	os.write(this.parameters.getFunctionCode());
        	os.write(00);
        	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode()+" 0x00");
        	if(i<16){
        		os.write(Integer.parseInt("0"+Integer.toHexString(i).toString().charAt(0),16));
        		crcadd ="0"+Integer.toHexString(i).toString().charAt(0);
    		    System.out.print(" 0x0"+Integer.toHexString(i).toString().charAt(0));
        	}
        	else{
	    	os.write(Integer.parseInt(Integer.toHexString(i).toString().charAt(0)+""+Integer.toHexString(i).toString().charAt(1),16));
		    crcadd=Integer.toHexString(i).toString().charAt(0)+""+Integer.toHexString(i).toString().charAt(1);
	    	System.out.print(" 0x"+Integer.toHexString(i).toString().charAt(0)+""+Integer.toHexString(i).toString().charAt(1));
		    }
        	os.write(00);
        	os.write(00);
        	System.out.print(" 0x00"+" 0x00");
		    
		    //check sum
        	ModbusCRC16 crc = new ModbusCRC16();
	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),00,Integer.parseInt(crcadd.charAt(0)+""+crcadd.charAt(1),16),00,00};
	        for (int d : data) {
	            crc.update(d);
	        }
	        byte[] byteStr = new byte[2];
	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
        	os.write(byteStr[0]);
        	os.write(byteStr[1]);
        	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
		    	System.out.println("");
        	
        	os.flush();
        	}
        } catch (IOException e6) {}
    }

    
    
    
    
    
    
    
    
    public void sendStop(){
        try {
//        	messageString="-";
//
//        	os.write(messageString.getBytes());
        	//os.write(Integer.parseInt(messageString));
        	
        	//sent start state to register add 0x05 value 0x01
        	os.write(this.parameters.getStationID());
        	os.write(this.parameters.getFunctionCode());
        	os.write(00);
        	os.write(01);
        	os.write(00);
        	os.write(00);
        	System.out.print("send: "+" 0x"+this.parameters.getStationID()+" 0x"+this.parameters.getFunctionCode()+" 0x00"+" 0x01"+" 0x00"+" 0x00");

			  //check sum
        	ModbusCRC16 crc = new ModbusCRC16();
	        int[] data = new int[]{this.parameters.getStationID(),this.parameters.getFunctionCode(),00,01,00,00};
	        for (int d : data) {
	            crc.update(d);
	        }
	        byte[] byteStr = new byte[2];
	        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
	        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);
        	os.write(byteStr[0]);
        	os.write(byteStr[1]);
        	System.out.printf(" 0x%02X 0x%02X\n", byteStr[0], byteStr[1]);
		    	System.out.println("");
        	
        	os.flush();
//        	System.out.println(messageString.toString());
        } catch (IOException e6) {}
    }

    /**
    Reports the open status of the port.
    @return true if port is open, false if port is closed.
    */
    public boolean isOpen() {
	return open;
    }

    /**
    Handles SerialPortEvents. The two types of SerialPortEvents that this
    program is registered to listen for are DATA_AVAILABLE and BI. During 
    DATA_AVAILABLE the port buffer is read until it is drained, when no more
    data is availble and 30ms has passed the method returns. When a BI
    event occurs the words BREAK RECEIVED are written to the messageAreaIn.
    */

    public void serialEvent(SerialPortEvent e) {
 	// Create a StringBuffer and int to receive input data.
	StringBuffer inputBuffer = new StringBuffer();
	int newData = 0;

	// Determine type of event.
	switch (e.getEventType()) {

	    // Read data until -1 is returned. If \r is received substitute
	    // \n for correct newline handling.
	    case SerialPortEvent.DATA_AVAILABLE:
		    while (newData != -1) {
		    	try {
		    	    newData = is.read();
			    if (newData == -1) {
				break;
			    }
			    if ('\r' == (char)newData) {
			   	inputBuffer.append('\n');
			    } else {
			    	inputBuffer.append((char)newData);
			    }
		    	} catch (IOException ex) {
		    	    System.err.println(ex);
		    	    return;
		      	}
			    if(inputBuffer.toString().equals(this.parameters.getStoptime()+"S"))
			    {
			    	sendStop();
			    }
   		    }
		// Append received data to messageAreaIn.
		messageAreaIn.append(new String(inputBuffer));
		break;

	    // If break event append BREAK RECEIVED message.
	    case SerialPortEvent.BI:
		messageAreaIn.append("\n--- BREAK RECEIVED ---\n");
	}

    }   

    /**
    Handles ownership events. If a PORT_OWNERSHIP_REQUESTED event is
    received a dialog box is created asking the user if they are 
    willing to give up the port. No action is taken on other types
    of ownership events.
    */
    public void ownershipChange(int type) {
	if (type == CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED) {
	    PortRequestedDialog prd = new PortRequestedDialog(parent);
	}
    }

    /**
    A class to handle <code>KeyEvent</code>s generated by the messageAreaOut.
    When a <code>KeyEvent</code> occurs the <code>char</code> that is 
    generated by the event is read, converted to an <code>int</code> and 
    writen to the <code>OutputStream</code> for the port.
    */
    class KeyHandler extends KeyAdapter {
	OutputStream os;

	/**
	Creates the KeyHandler.
	@param os The OutputStream for the port.
	*/
	public KeyHandler(OutputStream os) {
	    super();
	    this.os = os;
	}

	/**
	Handles the KeyEvent.
	Gets the <code>char</char> generated by the <code>KeyEvent</code>,
	converts it to an <code>int</code>, writes it to the <code>
	OutputStream</code> for the port.
	*/
        public void keyTyped(KeyEvent evt) {
            char newCharacter = evt.getKeyChar();
	    try {
	    	os.write((int)newCharacter);
	    } catch (IOException e) {
		System.err.println("OutputStream write error: " + e);
	    }
        }
    }
}
