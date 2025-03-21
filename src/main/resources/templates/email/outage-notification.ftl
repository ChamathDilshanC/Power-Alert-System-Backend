<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
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
        .outage-type {
            font-weight: bold;
            color: #cc0000;
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
        .button {
            display: inline-block;
            background-color: #0066cc;
            color: white;
            text-decoration: none;
            padding: 10px 20px;
            border-radius: 5px;
            margin-top: 15px;
        }
        .scheduled {
            color: #ff9900;
        }
        .ongoing {
            color: #cc0000;
        }
        .completed {
            color: #009900;
        }
        .cancelled {
            color: #666666;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1>${title}</h1>
    </div>
    <div class="content">
        <p>${greeting},</p>

        <p>${message}</p>

        <div class="outage-info">
            <p><strong>${outageTypeLabel}:</strong> <span class="outage-type">${outageType}</span></p>
            <p><strong>${areaLabel}:</strong> ${area}</p>
            <p><strong>${statusLabel}:</strong>
                <#if status == "SCHEDULED">
                    <span class="scheduled">${scheduledLabel}</span>
                <#elseif status == "ONGOING">
                    <span class="ongoing">${ongoingLabel}</span>
                <#elseif status == "COMPLETED">
                    <span class="completed">${completedLabel}</span>
                <#elseif status == "CANCELLED">
                    <span class="cancelled">${cancelledLabel}</span>
                <#else>
                    ${status}
                </#if>
            </p>
            <#if startTime??>
                <p><strong>${startTimeLabel}:</strong> ${startTime}</p>
            </#if>
            <#if endTime??>
                <p><strong>${endTimeLabel}:</strong> ${endTime}</p>
            </#if>
            <#if reason?? && reason != "">
                <p><strong>${reasonLabel}:</strong> ${reason}</p>
            </#if>
        </div>

        <p>${additionalInfo}</p>

        <p>
            <a href="${portalUrl}" class="button">${viewDetailsLabel}</a>
        </p>
    </div>
    <div class="footer">
        <p>${footerText}</p>
        <p>${unsubscribeText} <a href="${unsubscribeUrl}">${unsubscribeHereLabel}</a></p>
        <p>&copy; ${year} Power Alert</p>
    </div>
</div>
</body>
</html>