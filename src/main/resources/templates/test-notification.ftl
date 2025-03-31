<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PowerAlert Test Notification</title>
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
            background-color: #4285F4;
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

        .test-card {
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 20px;
            margin: 20px 0;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        }

        .test-icon {
            width: 40px;
            height: 40px;
            margin-bottom: 15px;
            display: block;
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
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1>PowerAlert Test Notification</h1>
    </div>

    <div class="content">
        <p>Hello ${username},</p>

        <p>This is a test notification from PowerAlert system.</p>

        <div class="test-card">
            <svg xmlns="http://www.w3.org/2000/svg" class="test-icon" fill="none" viewBox="0 0 24 24" stroke="#4285F4">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>

            <p><strong>Test Message:</strong></p>
            <p>${testMessage!''}</p>

            <#if outageType??>
                <p><strong>Outage Type:</strong> ${outageType}</p>
            </#if>

            <#if areaName??>
                <p><strong>Area:</strong> ${areaName}</p>
            </#if>

            <p><strong>Time:</strong> ${.now?string('yyyy-MM-dd HH:mm:ss')}</p>
        </div>

        <p>This is just a test notification. No action is required.</p>
    </div>

    <div class="footer">
        <p>This is an automated message from Power Alert. Please do not reply to this email.</p>
        <p>If you need further assistance, please contact our customer service at support@poweralert.lk</p>
        <p>&copy; ${.now?string('yyyy')} Power Alert. All rights reserved.</p>

        <div class="social-links">
            <a href="https://facebook.com/poweralert">Facebook</a> •
            <a href="https://twitter.com/poweralert">Twitter</a> •
            <a href="https://poweralert.lk/preferences">Preferences</a>
        </div>
    </div>
</div>
</body>
</html>