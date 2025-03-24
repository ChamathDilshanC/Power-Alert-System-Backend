<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PowerAlert Login Notification</title>
    <style>
        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 0;
            padding: 0;
            background-color: #ffffff;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
        }
        .logo {
            text-align: center;
            margin-bottom: 20px;
        }
        .logo img {
            width: 100px;
            height: auto;
        }
        .header {
            text-align: center;
            padding: 10px 0;
        }
        .header h1 {
            font-size: 24px;
            margin: 0;
            color: #202124;
        }
        .header h2 {
            font-size: 18px;
            font-weight: normal;
            margin: 10px 0;
            color: #202124;
        }
        .content {
            background-color: white;
            padding: 20px 0;
        }
        .login-info {
            background-color: #f8f9fa;
            border: 1px solid #f1f3f4;
            border-radius: 5px;
            padding: 20px;
            margin: 20px 0;
        }
        .button-container {
            text-align: center;
            margin: 25px 0;
        }
        .button {
            display: inline-block;
            background-color: #1a73e8;
            color: white;
            text-decoration: none;
            padding: 10px 24px;
            border-radius: 4px;
            font-size: 14px;
            font-weight: 500;
        }
        .footer {
            text-align: center;
            font-size: 12px;
            color: #5f6368;
            margin-top: 20px;
        }
        hr {
            border: none;
            height: 1px;
            background-color: #e0e0e0;
            margin: 20px 0;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="logo">
        <img src="https://i.pinimg.com/736x/60/10/1e/60101ee7d062f3c2d3d60919e0e5cc06.jpg" alt="PowerAlert Logo">
    </div>

    <div class="header">
        <h1>Hi ${username},</h1>
        <h2>New login to your PowerAlert account</h2>
    </div>

    <hr>

    <div class="content">
        <p>We detected a new sign-in to your PowerAlert account on your device.</p>

        <div class="login-info">
            <p><strong>Username:</strong> ${username}</p>
            <p><strong>Time:</strong> ${loginTime}</p>
            <p><strong>Device:</strong> ${device}</p>
        </div>

        <p>If this was you, you can safely ignore this email. If you don't recognize this activity, please secure your account immediately.</p>

        <div class="button-container">
            <a href="${accountSecurityUrl}" class="button">Check activity</a>
        </div>
    </div>

    <hr>

    <div class="footer">
        <p>This email was sent to ${email!'your account'} to let you know about important changes to your PowerAlert Account and services.</p>
        <p>© ${year} PowerAlert, Colombo, Sri Lanka</p>
        <p>If you don't want to receive emails to help set up your device with PowerAlert, please <a href="${unsubscribeUrl}" style="color: #1a73e8;">unsubscribe</a>.</p>
    </div>
</div>
</body>
</html>