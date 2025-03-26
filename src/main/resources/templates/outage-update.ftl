<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${outageType} Outage Update</title>
    <style>
        /* Base styles */
        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 0;
            padding: 0;
            background-color: #f9f9f9;
        }

        .container {
            max-width: 600px;
            margin: 0 auto;
            padding: 0;
            background-color: #ffffff;
        }

        .header {
            background-color: #0E7490;
            color: white;
            padding: 20px;
            text-align: center;
            border-radius: 8px 8px 0 0;
        }

        .header h1 {
            margin: 0;
            font-size: 24px;
            font-weight: 600;
        }

        .content {
            padding: 30px;
            background-color: #ffffff;
        }

        .outage-card {
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 20px;
            margin: 20px 0;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        }

        .alert-icon {
            width: 40px;
            height: 40px;
            margin-bottom: 15px;
            display: block;
        }

        .outage-label {
            font-weight: 600;
            margin-bottom: 5px;
            color: #64748b;
        }

        .outage-value {
            margin-top: 0;
            margin-bottom: 15px;
            color: #334155;
        }

        .status-badge {
            display: inline-block;
            padding: 5px 12px;
            border-radius: 16px;
            font-size: 14px;
            font-weight: 600;
            background-color: #ECFEFF;
            color: #0E7490;
        }

        .btn-action {
            display: inline-block;
            background-color: #0E7490;
            color: white;
            padding: 12px 24px;
            border-radius: 6px;
            text-decoration: none;
            font-weight: 600;
            margin-top: 20px;
            transition: background-color 0.2s;
        }

        .btn-action:hover {
            background-color: #0c697d;
        }

        .footer {
            background-color: #f8fafc;
            padding: 20px;
            text-align: center;
            font-size: 12px;
            color: #64748b;
            border-radius: 0 0 8px 8px;
            border-top: 1px solid #e2e8f0;
        }

        .social-links {
            margin-top: 15px;
        }

        .social-links a {
            display: inline-block;
            margin: 0 8px;
            color: #64748b;
            text-decoration: none;
        }

        .update-info {
            background-color: #ECFEFF;
            border-left: 4px solid #0E7490;
            padding: 15px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1>${outageType} Outage Update</h1>
    </div>

    <div class="content">
        <p>Hello ${username},</p>

        <p>We have an important update regarding the previously notified <strong>${outageType}</strong> outage in your area: <strong>${areaName}</strong>.</p>

        <#if updateInfo??>
            <div class="update-info">
                <p><strong>Update:</strong> ${updateInfo}</p>
                <#if updateReason??>
                    <p><strong>Reason:</strong> ${updateReason}</p>
                </#if>
            </div>
        </#if>

        <div class="outage-card">
            <svg xmlns="http://www.w3.org/2000/svg" class="alert-icon" fill="none" viewBox="0 0 24 24" stroke="#0E7490">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>

            <p class="outage-label">Type:</p>
            <p class="outage-value">${outageType}</p>

            <p class="outage-label">Area:</p>
            <p class="outage-value">${areaName}</p>

            <p class="outage-label">Status:</p>
            <p class="outage-value">
                <span class="status-badge">${status}</span>
            </p>

            <p class="outage-label">Start Time:</p>
            <p class="outage-value">${startTime}</p>

            <#if endTime??>
                <p class="outage-label">Estimated End Time:</p>
                <p class="outage-value">${endTime}</p>
            </#if>

            <p class="outage-label">Last Updated:</p>
            <p class="outage-value">${updatedAt}</p>
        </div>

        <p>Please continue to plan accordingly. We'll provide further updates as they become available.</p>

        <a href="${portalUrl}" class="btn-action">View Full Details</a>

    </div>

    <div class="footer">
        <p>This is an automated message from Power Alert. Please do not reply to this email.</p>
        <p>If you need further assistance, please contact our customer service at support@poweralert.lk</p>
        <p>&copy; ${.now?string('yyyy')} Power Alert. All rights reserved.</p>

        <div class="social-links">
            <a href="https://facebook.com/poweralert">Facebook</a>
            <a href="https://twitter.com/poweralert">Twitter</a>
            <a href="https://poweralert.lk/preferences">Preferences</a>
        </div>
    </div>
</div>
</body>
</html>