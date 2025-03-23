<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${outageType} Outage Alert</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 0;
            padding: 0;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background-color: #0066cc;
            color: white;
            padding: 15px;
            text-align: center;
            border-radius: 5px 5px 0 0;
        }
        .content {
            background-color: #f9f9f9;
            padding: 20px;
            border-left: 1px solid #ddd;
            border-right: 1px solid #ddd;
        }
        .outage-info {
            background-color: white;
            border: 1px solid #ddd;
            border-radius: 5px;
            padding: 15px;
            margin-bottom: 20px;
        }
        .footer {
            background-color: #eee;
            padding: 15px;
            text-align: center;
            font-size: 12px;
            color: #666;
            border-radius: 0 0 5px 5px;
            border: 1px solid #ddd;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1>${outageType} Outage Alert</h1>
    </div>
    <div class="content">
        <p>Hello ${username},</p>

        <p>This is to inform you about a ${outageType} outage affecting your area: ${areaName}.</p>

        <div class="outage-info">
            <p><strong>Type:</strong> ${outageType}</p>
            <p><strong>Area:</strong> ${areaName}</p>
            <p><strong>Status:</strong> ${status}</p>
            <p><strong>Start Time:</strong> ${startTime}</p>
            <#if endTime??>
                <p><strong>Estimated End Time:</strong> ${endTime}</p>
            </#if>
            <#if reason??>
                <p><strong>Reason:</strong> ${reason}</p>
            </#if>
        </div>

        <p>Please plan accordingly. We apologize for any inconvenience this may cause.</p>
    </div>
    <div class="footer">
        <p>This is an automated message from Power Alert. Please do not reply to this email.</p>
        <p>&copy; ${year} Power Alert</p>
    </div>
</div>
</body>
</html>