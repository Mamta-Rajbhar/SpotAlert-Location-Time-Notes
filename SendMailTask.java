package com.example.location;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMailTask extends AsyncTask<Void, Void, Boolean> {
    private String recipientEmail;
    private String subject;
    private String messageBody;

    public SendMailTask(String recipientEmail, String subject, String messageBody) {
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.messageBody = messageBody;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        final String senderEmail = "spotalertteam@gmail.com";
        final String senderPassword = "enjhjipveufnzxju"; // Replace with your App Password

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            Log.d("SendMailTask", "Email sent successfully");
            return true; // Email sent successfully
        } catch (MessagingException e) {
            Log.e("SendMailTask", "Error sending email: " + e.getMessage());
            e.printStackTrace();
            return false; // Email failed
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            Log.d("SendMailTask", "Email sent successfully");
        } else {
            Log.e("SendMailTask", "Failed to send email");
        }
    }
}