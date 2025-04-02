<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Power Outage Notification</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 0;
            padding: 0;
            background-color: #f9f9f9;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }
        .header {
            background-color: #0055a4;
            color: white;
            padding: 20px;
            text-align: center;
        }
        .logo {
            margin-bottom: 10px;
            max-width: 180px;
        }
        .content {
            padding: 30px;
        }
        .footer {
            background-color: #f0f0f0;
            padding: 15px;
            text-align: center;
            font-size: 12px;
            color: #666;
        }
        h1 {
            margin: 0;
            font-size: 24px;
            font-weight: 600;
        }
        h2 {
            font-size: 20px;
            color: #0055a4;
            margin-top: 0;
        }
        .alert-info {
            background-color: #f5f9fc;
            border-left: 4px solid #0055a4;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
        }
        .outage-details {
            margin: 20px 0;
        }
        .outage-details table {
            width: 100%;
            border-collapse: collapse;
        }
        .outage-details table td {
            padding: 8px 0;
            border-bottom: 1px solid #eee;
        }
        .outage-details table td:first-child {
            font-weight: 600;
            width: 40%;
        }
        .button {
            display: inline-block;
            background-color: #fdfdfd;
            color: #000000;
            text-decoration: none;
            padding: 12px 25px;
            border-radius: 4px;
            margin-top: 20px;
            font-weight: 500;
        }
        .tips {
            margin-top: 25px;
            background-color: #f8f9fa;
            padding: 15px;
            border-radius: 4px;
        }
        .tips h3 {
            margin-top: 0;
            color: #555;
            font-size: 16px;
        }
        .tips ul {
            margin: 0;
            padding-left: 20px;
        }
        @media only screen and (max-width: 600px) {
            .content {
                padding: 20px;
            }
            h1 {
                font-size: 20px;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <#if logoUrl??>
            <img src="${logoUrl}" alt="Power Alert Logo" class="logo">
        <#else>
            <h1>Power Alert</h1>
        </#if>
        <h2>Outage Notification</h2>
    </div>

    <div class="content">
        <p>Hello ${username},</p>

        <p>This is to inform you about a scheduled <strong>${outageType}</strong> outage in your area.</p>

        <div class="alert-info">
            <p>The outage will affect <strong>${areaName}</strong> and surrounding areas.</p>
        </div>

        <div class="outage-details">
            <h3>Outage Details:</h3>
            <table>
                <tr>
                    <td>Status:</td>
                    <td>${status}</td>
                </tr>
                <tr>
                    <td>Start Time:</td>
                    <td>${startTime}</td>
                </tr>
                <#if endTime??>
                    <tr>
                        <td>Estimated End Time:</td>
                        <td>${endTime}</td>
                    </tr>
                </#if>
                <#if reason??>
                    <tr>
                        <td>Reason:</td>
                        <td>${reason}</td>
                    </tr>
                </#if>
                <#if additionalInfo??>
                    <tr>
                        <td>Additional Information:</td>
                        <td>${additionalInfo}</td>
                    </tr>
                </#if>
            </table>
        </div>

        <#if portalUrl??>
            <p>
                <a href="${portalUrl}" class="button">View Outage Details</a>
            </p>
        </#if>

        <div class="tips">
            <h3>During the outage:</h3>
            <ul>
                <li>Keep refrigerator and freezer doors closed</li>
                <li>Turn off and unplug sensitive electronics</li>
                <li>Have emergency lights or candles ready</li>
                <li>Check on elderly neighbors or those with medical needs</li>
            </ul>
        </div>

        <p>We apologize for any inconvenience this may cause and appreciate your patience.</p>

        <p>Thank you,<br>The Power Alert Team</p>
    </div>

    <div class="footer">
        <p>This is an automated notification. Please do not reply to this email.</p>
        <p>&copy; 2025 Power Alert | <a href="https://poweralert.lk">poweralert.lk</a></p>
    </div>
</div>
</body>
</html>