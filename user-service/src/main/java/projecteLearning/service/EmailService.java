package projecteLearning.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import projecteLearning.exception.EmailSendingException;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendVerificationCode(String toEmail, String firstName, String code, long expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress, "Omarise");
            helper.setTo(toEmail);
            helper.setSubject("Verify your Omarise account");
            helper.setText(buildEmailContent(firstName, code, expirationMinutes), true);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new EmailSendingException("Failed to send verification email. Please try again later.");
        }
    }

    private String buildEmailContent(String firstName, String code, long expirationMinutes) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f2f4f7; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f2f4f7; padding:48px 16px;">
                    <tr>
                      <td align="center">
                        <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:12px; border:1px solid #e6e9ee; overflow:hidden;">

                          <!-- Header -->
                          <tr>
                            <td align="center" style="background-color:#1565c0; padding:36px 32px;">
                              <span style="color:#ffffff; font-size:24px; font-weight:800; letter-spacing:3px;">OMARISE</span>
                              <div style="width:36px; height:3px; background-color:rgba(255,255,255,0.55); margin:14px auto 0; border-radius:2px;"></div>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 36px 8px 36px;">
                              <p style="margin:0 0 8px 0; color:#1565c0; font-size:12px; font-weight:700; letter-spacing:1.5px; text-transform:uppercase;">
                                Account verification
                              </p>
                              <h1 style="margin:0 0 20px 0; color:#111827; font-size:22px; font-weight:700;">
                                Confirm your email address
                              </h1>
                              <p style="margin:0 0 28px 0; color:#4b5563; font-size:15px; line-height:1.6;">
                                Hi %s, thanks for creating an account with <strong>Omarise</strong>. Enter the verification code below to activate your account and get started.
                              </p>

                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                                <tr>
                                  <td align="center" style="background-color:#eef4fc; border-radius:8px; padding:22px;">
                                    <span style="font-family:'Courier New', Courier, monospace; font-size:30px; font-weight:700; letter-spacing:10px; color:#1565c0;">
                                      %s
                                    </span>
                                  </td>
                                </tr>
                              </table>

                              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb; border-left:3px solid #1565c0; border-radius:4px; margin-bottom:8px;">
                                <tr>
                                  <td style="padding:14px 16px;">
                                    <p style="margin:0; color:#6b7280; font-size:13px; line-height:1.5;">
                                      This code expires in <strong>%d minutes</strong>. For your security, never share it with anyone — Omarise staff will never ask you for it.
                                    </p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Divider -->
                          <tr>
                            <td style="padding:24px 36px 0 36px;">
                              <div style="height:1px; background-color:#e6e9ee;"></div>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td align="center" style="padding:24px 36px 32px 36px;">
                              <p style="margin:0 0 4px 0; color:#9ca3af; font-size:12px;">
                                Didn't create an Omarise account? You can safely ignore this email.
                              </p>
                              <p style="margin:0; color:#c1c6cf; font-size:12px;">
                                &copy; 2026 Omarise. All rights reserved.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, code, expirationMinutes);
    }


    public void sendPasswordResetCode(String toEmail, String firstName, String code, long expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress, "Omarise");
            helper.setTo(toEmail);
            helper.setSubject("Reset your Omarise password");
            helper.setText(buildPasswordResetEmailContent(firstName, code, expirationMinutes), true);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new EmailSendingException("Failed to send password reset email. Please try again later.");
        }
    }


    private String buildPasswordResetEmailContent(String firstName, String code, long expirationMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f2f4f7; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f2f4f7; padding:48px 16px;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:12px; border:1px solid #e6e9ee; overflow:hidden;">

                      <tr>
                        <td align="center" style="background-color:#1565c0; padding:36px 32px;">
                          <span style="color:#ffffff; font-size:24px; font-weight:800; letter-spacing:3px;">OMARISE</span>
                          <div style="width:36px; height:3px; background-color:rgba(255,255,255,0.55); margin:14px auto 0; border-radius:2px;"></div>
                        </td>
                      </tr>

                      <tr>
                        <td style="padding:40px 36px 8px 36px;">
                          <p style="margin:0 0 8px 0; color:#1565c0; font-size:12px; font-weight:700; letter-spacing:1.5px; text-transform:uppercase;">
                            Password reset
                          </p>
                          <h1 style="margin:0 0 20px 0; color:#111827; font-size:22px; font-weight:700;">
                            Reset your password
                          </h1>
                          <p style="margin:0 0 28px 0; color:#4b5563; font-size:15px; line-height:1.6;">
                            Hi %s, we received a request to reset your Omarise account password. Enter the code below to continue.
                          </p>

                          <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                            <tr>
                              <td align="center" style="background-color:#eef4fc; border-radius:8px; padding:22px;">
                                <span style="font-family:'Courier New', Courier, monospace; font-size:30px; font-weight:700; letter-spacing:10px; color:#1565c0;">
                                  %s
                                </span>
                              </td>
                            </tr>
                          </table>

                          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb; border-left:3px solid #1565c0; border-radius:4px; margin-bottom:8px;">
                            <tr>
                              <td style="padding:14px 16px;">
                                <p style="margin:0; color:#6b7280; font-size:13px; line-height:1.5;">
                                  This code expires in <strong>%d minutes</strong>. If you didn't request this, you can safely ignore this email — your password will remain unchanged.
                                </p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <tr>
                        <td style="padding:24px 36px 0 36px;">
                          <div style="height:1px; background-color:#e6e9ee;"></div>
                        </td>
                      </tr>

                      <tr>
                        <td align="center" style="padding:24px 36px 32px 36px;">
                          <p style="margin:0; color:#c1c6cf; font-size:12px;">
                            &copy; 2026 Omarise. All rights reserved.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(firstName, code, expirationMinutes);
    }

    public void sendAdminCreatedCredentials(String toEmail, String firstName, String plainPassword, String roleLabel) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress, "Omarise");
            helper.setTo(toEmail);
            helper.setSubject("Your Omarise account has been created");
            helper.setText(buildAdminCreatedEmailContent(firstName, toEmail, plainPassword, roleLabel), true);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new EmailSendingException("Failed to send account credentials email. Please try again later.");
        }
    }

    private String buildAdminCreatedEmailContent(String firstName, String email, String password, String roleLabel) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color:#f2f4f7; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f2f4f7; padding:48px 16px;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:12px; border:1px solid #e6e9ee; overflow:hidden;">
                      <tr>
                        <td align="center" style="background-color:#1565c0; padding:36px 32px;">
                          <span style="color:#ffffff; font-size:24px; font-weight:800; letter-spacing:3px;">OMARISE</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:40px 36px 8px 36px;">
                          <p style="margin:0 0 8px 0; color:#1565c0; font-size:12px; font-weight:700; letter-spacing:1.5px; text-transform:uppercase;">
                            Account created by an administrator
                          </p>
                          <h1 style="margin:0 0 20px 0; color:#111827; font-size:22px; font-weight:700;">
                            Welcome to Omarise, %s
                          </h1>
                          <p style="margin:0 0 24px 0; color:#4b5563; font-size:15px; line-height:1.6;">
                            An administrator has created a <strong>%s</strong> account for you on Omarise. Use the credentials below to sign in.
                          </p>

                          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#eef4fc; border-radius:8px; margin-bottom:24px;">
                            <tr>
                              <td style="padding:20px 22px;">
                                <p style="margin:0 0 10px 0; color:#374151; font-size:14px;">
                                  <strong>Email:</strong> %s
                                </p>
                                <p style="margin:0; color:#374151; font-size:14px;">
                                  <strong>Temporary password:</strong>
                                  <span style="font-family:'Courier New', Courier, monospace; font-size:16px; font-weight:700; color:#1565c0;">%s</span>
                                </p>
                              </td>
                            </tr>
                          </table>

                          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb; border-left:3px solid #1565c0; border-radius:4px; margin-bottom:8px;">
                            <tr>
                              <td style="padding:14px 16px;">
                                <p style="margin:0; color:#6b7280; font-size:13px; line-height:1.5;">
                                  For your security, please sign in and change this password as soon as possible. Never share it with anyone.
                                </p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:24px 36px 0 36px;">
                          <div style="height:1px; background-color:#e6e9ee;"></div>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:24px 36px 32px 36px;">
                          <p style="margin:0; color:#c1c6cf; font-size:12px;">
                            &copy; 2026 Omarise. All rights reserved.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(firstName, roleLabel, email, password);
    }

}