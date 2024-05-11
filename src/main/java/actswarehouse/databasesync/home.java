/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package actswarehouse.databasesync;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import com.jcraft.jsch.*;  
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.util.Timer;
import java.util.TimerTask;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.CronTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import actswarehouse.databasesync.JobScheduler;

/**
 *
 * @author server
 */
public class home extends javax.swing.JFrame {
    private static final String REMOTE_HOST = "34.27.44.120";  
    //variable for user name  
    private static final String USERNAME = "actsmis";  
    //variable for password  
    private static final String PASSWORD = "Dxdiag@2021";  
    //port number for SFTP  
    private static final int REMOTE_PORT = 22;  
    private static final int SESSION_TIMEOUT = 10000;  
    private static final int CHANNEL_TIMEOUT = 5000; 

    /**
     * Creates new form home
     */
    public home() {
        //Icon img = new ImageIcon(this.getClass().getResource("/line.gif")).getImage();
        //Icon icon = new ImageIcon("/resources/line.gif");
        //jLabel5.setIcon(icon);
        initComponents();
        //preparedatabase();
        //scheduleuploads for continuously running servers
    }
    
    public void scheduleuploads(){
        try {
 
         // specify the job' s details..
         JobDetail job = JobBuilder.newJob(JobScheduler.class)
                                   .build();
 
         // specify the running period of the job
         Trigger trigger = TriggerBuilder.newTrigger()
                                         .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                                                            .withIntervalInSeconds(21600)
                                                                            .repeatForever())
                                          .build();
         //schedule the job
         SchedulerFactory schFactory = new StdSchedulerFactory();
         Scheduler sch = schFactory.getScheduler();
         sch.start();
         sch.scheduleJob(job, trigger);
 
      } catch (SchedulerException e) {
         e.printStackTrace();
      }
    }
    
    public String startprocessingupload(){
        //String processstatus = "";
        preparedatabase();
        System.out.println("Database Upload Executed Successfully");
        return "success";
    }
    
    public void preparedatabase(){
        /*** change text status ***/
        jLabel1.setForeground(new Color(37, 101, 37));
        jLabel1.setText("Preparing Database for upload...");
        
        /*** identify the latest file ***/
        File directory = new File("/var/backups/KenyaEMR");
        directory.setReadable(true, false);
        directory.setExecutable(true, false);
        directory.setWritable(true, false);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null)
        {
            for (File file : files)
            {
                if (file.lastModified() > lastModifiedTime)
                {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }
        
        String databasename = chosenFile.getName().toString();
        jLabel1.setText(databasename);
        
        /*** create database sync folder at home ***/
        String homefolder = System.getProperty("user.home");
        String path = homefolder+"/databasesync";
        File folder = new File(path);
        if (!folder.exists()) {
                folder.mkdir();
        }
        
        /*** clear the folder if there are existing files ***/
        File databasesyncdirectory = new File(homefolder+"/databasesync");
        for(File file: databasesyncdirectory.listFiles()){
            if (!file.isDirectory()){
                file.delete();
            } 
        }
            
        
        /*** copy selected database to the created folder ***/
        String postdbname = getAlphaNumericString(5);
        File source = new File("/var/backups/KenyaEMR/"+databasename);
        File dest = new File(homefolder+"/databasesync/openmrs"+postdbname+".gz");
        String finaldatabasename = "openmrs"+postdbname+".gz";
        try {
            FileUtils.copyFile(source, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        jLabel1.setText("Preparing Database for upload successful.");
        Timer timer = new Timer();
        timer.schedule( new TimerTask() {
            public void run() {
               // do your work 
               String serverstatus = checkserverifreachable();
               if(serverstatus == "success"){
                   System.out.println("Karibu ina upload");
                   uploaddatabasetoserver(finaldatabasename);
                   timer.cancel();
               }
            }
         }, 0, 60*1000);
        
        //check if internet is available
    }
    
    
    
    public void uploaddatabasetoserver(String finaldatabasename){
        jLabel2.setForeground(new Color(37, 101, 37));
        jLabel2.setText("Uploading database to the server...");
        String homefolder = System.getProperty("user.home");
        String localFile = homefolder+"/databasesync/"+finaldatabasename;  
        String remoteFile = "/var/www/html/actsmis/dbuploads";  
        Session jschSession = null;  
        try   
        {  
            JSch jsch = new JSch();  
            jsch.setKnownHosts("~/.ssh/known_hosts"); 
            java.util.Properties config = new java.util.Properties(); 
            config.put("StrictHostKeyChecking", "no");
            jsch.setConfig(config);
            jschSession = jsch.getSession(USERNAME, REMOTE_HOST, REMOTE_PORT);  
        // authenticate using private key  
        // jsch.addIdentity("/home/javatpoint/.ssh/id_rsa");  
        // authenticate using password  
            jschSession.setPassword(PASSWORD);  
        // 10 seconds session timeout  
            jschSession.connect(SESSION_TIMEOUT);  
            Channel sftp = jschSession.openChannel("sftp");  
        // 5 seconds timeout  
            sftp.connect(CHANNEL_TIMEOUT);  
            ChannelSftp channelSftp = (ChannelSftp) sftp;  
        // transfer file from local to remote server  
            channelSftp.put(localFile, remoteFile);  
        // download file from remote server to local  
        // channelSftp.get(remoteFile, localFile);  
            channelSftp.exit();  
        }   
        catch (JSchException | SftpException e)   
        {  
            System.out.println("Error1: " + e.getMessage());
            e.printStackTrace();  
        }   
        finally   
        {  
            if (jschSession != null)   
            {  
                jschSession.disconnect();  
            }  
        }  
        jLabel2.setText("Database uploaded successfully");
        jLabel3.setText("Database Restored successfully");
        
        restoredatabase(finaldatabasename);
    }
    
    public void restoredatabase(String finaldatabasename){
        String GET_URL = "http://34.27.44.120/restoredatabase/"+finaldatabasename;
        String USER_AGENT = "Mozilla/5.0";
        try{
            URL obj = new URL(GET_URL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setConnectTimeout(0);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = con.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                    }
                    in.close();

                    // print result
                    System.out.println("Code imefika hapa.");
                    System.out.println(response.toString());
            } else {
                    System.out.println("Code imechapa.");
                    System.out.println("GET request did not work.");
            }
        }catch(IOException  e){
            System.out.println("Apierror: " + e.getMessage());
            e.printStackTrace();  
        }
    }
    
    public String checkserverifreachable(){
        String GET_URL = "http://34.27.44.120/restservices/systemstatus";
        String USER_AGENT = "Mozilla/5.0";
        String systemstatus = "";
        try{
            URL obj = new URL(GET_URL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setConnectTimeout(0);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = con.getResponseCode();
            System.out.println("Wacha tuangalie internet.");
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                systemstatus = "success";
            } else {
                systemstatus = "error";
            }
        }catch(IOException  e){
            systemstatus = "error";
        }
        return systemstatus;
    }
    
    /*** function to generate random string ***/
    static String getAlphaNumericString(int n)
    {

     // chose a Character random from this String
     String AlphaNumericString = "abcdefghijklmnopqrstuvxyz";

     // create StringBuffer size of AlphaNumericString
     StringBuilder sb = new StringBuilder(n);

     for (int i = 0; i < n; i++) {

      // generate a random number between
      // 0 to AlphaNumericString variable length
      int index
       = (int)(AlphaNumericString.length()
         * Math.random());

      // add Character one by one in end of sb
      sb.append(AlphaNumericString
         .charAt(index));
     }

     return sb.toString();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Database Sync Tool");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });

        jLabel1.setForeground(new java.awt.Color(153, 153, 153));
        jLabel1.setText("Preparing Database for upload");

        jLabel2.setForeground(new java.awt.Color(153, 153, 153));
        jLabel2.setText("Uploading Database to the Server");

        jLabel3.setForeground(new java.awt.Color(153, 153, 153));
        jLabel3.setText("Restoring Database");

        jLabel4.setForeground(new java.awt.Color(153, 153, 153));
        jLabel4.setText("View Reports");

        jLabel6.setFont(new java.awt.Font("Liberation Sans", 1, 18)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Please Don't Close this windows");
        jLabel6.addContainerListener(new java.awt.event.ContainerAdapter() {
            public void componentAdded(java.awt.event.ContainerEvent evt) {
                jLabel6ComponentAdded(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addComponent(jLabel6)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(jLabel6)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addContainerGap(76, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        jLabel1.setText("Preparing Database for upload...");
        //preparedatabase();
        scheduleuploads();
    }//GEN-LAST:event_formWindowOpened

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        // TODO add your handling code here:
        //preparedatabase();
    }//GEN-LAST:event_formWindowActivated

    private void formComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentShown
        // TODO add your handling code here:
        //preparedatabase();
    }//GEN-LAST:event_formComponentShown

    private void jLabel6ComponentAdded(java.awt.event.ContainerEvent evt) {//GEN-FIRST:event_jLabel6ComponentAdded
        // TODO add your handling code here:
        //preparedatabase();
    }//GEN-LAST:event_jLabel6ComponentAdded

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new home().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
