package app.dya.service;

import app.dya.api.dto.AlertItem;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Service responsible for sending alert emails via SendGrid.
 * Requires SENDGRID_API_KEY environment variable to be set.
 */
@Service
public class EmailAlertService {

    private final SendGrid sendGrid;
    private final String fromEmail;

    public EmailAlertService() {
        String apiKey = System.getenv("SENDGRID_API_KEY");
        this.fromEmail = System.getenv().getOrDefault("ALERT_FROM_EMAIL", "alerts@example.com");
        this.sendGrid = (apiKey == null || apiKey.isBlank()) ? null : new SendGrid(apiKey);
    }

    public void send(String to, List<AlertItem> alerts) {
        if (sendGrid == null || to == null || alerts == null || alerts.isEmpty()) {
            return;
        }
        StringBuilder body = new StringBuilder();
        for (AlertItem a : alerts) {
            body.append("[").append(a.type()).append("] ")
                .append(a.message()).append(" (")
                .append(a.protocol()).append(")\n");
        }
        Email from = new Email(fromEmail);
        Email recipient = new Email(to);
        Mail mail = new Mail(from, "DYA Alerts", recipient, new Content("text/plain", body.toString()));
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        try {
            request.setBody(mail.build());
            sendGrid.api(request);
        } catch (IOException ignored) {
            // silently ignore failures in this MVP
        }
    }
}
