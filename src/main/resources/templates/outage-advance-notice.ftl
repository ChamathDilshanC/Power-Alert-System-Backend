<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Advance Notice: ${outageType} Outage</title>
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
            background-color: #8B5CF6;
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

        .advance-notice-banner {
            background-color: #F5F3FF;
            padding: 15px;
            border-left: 4px solid #8B5CF6;
            margin-bottom: 20px;
            border-radius: 0 6px 6px 0;
        }

        .countdown {
            margin: 0;
            font-weight: 600;
            color: #7C3AED;
            font-size: 18px;
        }

        .outage-card {
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 20px;
            margin: 20px 0;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        }

        .clock-icon {
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
            background-color: #F5F3FF;
            color: #7C3AED;
        }

        .checklist {
            background-color: #F8FAFC;
            border-radius: 8px;
            padding: 20px;
            margin-top: 20px;
        }

        .checklist h3 {
            margin-top: 0;
            color: #334155;
        }

        .checklist ul {
            margin-bottom: 0;
            padding-left: 20px;
        }

        .checklist li {
            margin-bottom: 8px;
            color: #475569;
        }

        .btn-action {
            display: inline-block;
            background-color: #8B5CF6;
            color: white;
            padding: 12px 24px;
            border-radius: 6px;
            text-decoration: none;
            font-weight: 600;
            margin-top: 20px;
            transition: background-color 0.2s;
        }

        .btn-action:hover {
            background-color: #7C3AED;
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
        <h1>Advance Notice: ${outageType} Outage</h1>
    </div>

    <div class="content">
        <p>Hello ${username},</p>

        <div class="advance-notice-banner">
            <p class="countdown">⏰ ${hoursUntilStart} hours until scheduled outage begins</p>
        </div>

        <p>This is an advance reminder about the scheduled ${outageType} outage that will affect your area <strong>${areaName}</strong> soon.</p>

        <div class="outage-card">
            <svg xmlns="http://www.w3.org/2000/svg" class="clock-icon" fill="none" viewBox="0 0 24 24" stroke="#8B5CF6">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>

            <p class="outage-label">Type:</p>
            <p class="outage-value">${outageType}</p>

            <p class="outage-label">Area:</p>
            <p class="outage-value">${areaName}</p>

            <p class="outage-label">Status:</p>
            <p class="outage-value">
                <span class="status-badge">UPCOMING</span>
            </p>

            <p class="outage-label">Start Time:</p>
            <p class="outage-value">${startTime}</p>

            <#if endTime??>
                <p class="outage-label">Estimated End Time:</p>
                <p class="outage-value">${endTime}</p>
            </#if>

            <#if reason??>
                <p class="outage-label">Reason:</p>
                <p class="outage-value">${reason}</p>
            </#if>
        </div>

        <div class="checklist">
            <h3>Preparation Checklist</h3>
            <ul>
                <#if outageType == "ELECTRICITY">
                    <li>Charge all essential electronic devices</li>
                    <li>Prepare battery-powered lighting</li>
                    <li>Keep refrigerator doors closed to maintain temperature</li>
                    <li>Consider unplugging sensitive electronic equipment</li>
                    <li>Have backup power banks ready</li>
                <#elseif outageType == "WATER">
                    <li>Store drinking water in clean containers</li>
                    <li>Fill bathtubs for non-drinking water use</li>
                    <li>Prepare ready-to-eat meals that don't require water</li>
                    <li>Delay laundry and other water-intensive activities</li>
                    <li>Keep bottled water on hand</li>
                <#elseif outageType == "GAS">
                    <li>Turn off gas-powered appliances before the outage</li>
                    <li>Have alternative cooking methods available</li>
                    <li>Check that gas detectors have working batteries</li>
                    <li>Plan for alternative heating if needed</li>
                    <li>Prepare meals that don't require cooking</li>
                </#if>
            </ul>
        </div>

        <p>We appreciate your patience and understanding as we work to maintain and improve our infrastructure.</p>

        <a href="${portalUrl}" class="btn-action">View Details</a>

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