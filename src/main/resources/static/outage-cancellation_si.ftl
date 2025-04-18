<!DOCTYPE html>
<html lang="si">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${outageType} විදුලි විසන්ධිය අවලංගු කිරීම</title>
    <style>
        :root {
            --primary-color: #DC2626;
            --primary-light: #FEE2E2;
            --text-dark: #334155;
            --text-muted: #64748b;
            --bg-light: #f8fafc;
            --border-color: #e2e8f0;
            --shadow: 0 2px 4px rgba(0,0,0,0.05);
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            line-height: 1.6;
            color: var(--text-dark);
            background-color: #f5f5f5;
        }

        .container {
            max-width: 600px;
            margin: 20px auto;
            border-radius: 12px;
            overflow: hidden;
            background-color: #ffffff;
            box-shadow: 0 4px 12px rgba(0,0,0,0.08);
        }

        .header {
            background-color: var(--primary-color);
            color: white;
            padding: 24px 20px;
            text-align: center;
        }

        .header h1 {
            margin: 0;
            font-size: 22px;
            font-weight: 600;
        }

        .content {
            padding: 30px;
            color: var(--text-dark);
        }

        .greeting {
            font-size: 18px;
            margin-bottom: 20px;
        }

        .outage-card {
            background-color: var(--bg-light);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            padding: 24px;
            margin: 24px 0;
            box-shadow: var(--shadow);
        }

        .outage-label {
            font-weight: 600;
            margin-bottom: 5px;
            color: var(--text-muted);
            font-size: 14px;
        }

        .outage-value {
            margin-top: 0;
            margin-bottom: 16px;
            color: var(--text-dark);
            font-size: 16px;
        }

        .status-badge {
            display: inline-block;
            padding: 6px 12px;
            border-radius: 16px;
            font-size: 14px;
            font-weight: 600;
            background-color: var(--primary-light);
            color: var(--primary-color);
        }

        .footer {
            background-color: var(--bg-light);
            padding: 24px;
            text-align: center;
            font-size: 13px;
            color: var(--text-muted);
            border-top: 1px solid var(--border-color);
        }

        .social-links {
            margin-top: 16px;
        }

        .social-links a {
            display: inline-block;
            margin: 0 8px;
            color: var(--primary-color);
            text-decoration: none;
        }

        @media (max-width: 640px) {
            .container {
                margin: 0;
                border-radius: 0;
            }

            .content {
                padding: 20px;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1>${outageType} විදුලි විසන්ධිය අවලංගු කර ඇත</h1>
    </div>

    <div class="content">
        <p class="greeting">ආයුබෝවන් ${username},</p>

        <p>ඔබගේ ප්‍රදේශය: <strong>${areaName}</strong> සඳහා සැලසුම් කර තිබූ <strong>${outageType}</strong> විදුලි විසන්ධිය <strong>අවලංගු කර</strong> ඇති බව ඔබට දන්වනු කැමැත්තෙමු.</p>

        <div class="outage-card">
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#DC2626" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 18L18 6M6 6l12 12" />
            </svg>

            <p class="outage-label">වර්ගය:</p>
            <p class="outage-value">${outageType}</p>

            <p class="outage-label">ප්‍රදේශය:</p>
            <p class="outage-value">${areaName}</p>

            <p class="outage-label">තත්ත්වය:</p>
            <p class="outage-value">
                <span class="status-badge">අවලංගු කර ඇත</span>
            </p>

            <p class="outage-label">මුල් ආරම්භක වේලාව:</p>
            <p class="outage-value">${startTime}</p>

            <#if cancellationReason??>
                <p class="outage-label">අවලංගු කිරීමට හේතුව:</p>
                <p class="outage-value">${cancellationReason}</p>S
            </#if>
        </div>

        <p>දැන් ඔබට සාමාන්‍ය පරිදි සේවාවන් භාවිතා කළ හැකිය. කිසිදු බාධාවක් අපේක්ෂා නොකරන්න.</p>

        <p>පෙර දැනුම්දීම හේතුවෙන් ඇති විය හැකි අපහසුතා සඳහා අපි සමාව අයැද සිටිමු.</p>
    </div>

    <div class="footer">
        <p>මෙය Power Alert වෙතින් ස්වයංක්‍රීයව යැවූ පණිවිඩයකි. කරුණාකර මෙම විද්‍යුත් තැපෑලට පිළිතුරු නොදෙන්න.</p>
        <p>ඔබට වැඩිදුර සහාය අවශ්‍ය නම්, කරුණාකර අපගේ පාරිභෝගික සේවාව අමතන්න support@poweralert.lk</p>
        <p>&copy; ${.now?string('yyyy')} Power Alert. සියලුම හිමිකම් ඇවිරිණි.</p>

        <div class="social-links">
            <a href="https://facebook.com/poweralert">Facebook</a> •
            <a href="https://twitter.com/poweralert">Twitter</a> •
            <a href="https://poweralert.lk/preferences">වරණයන්</a>
        </div>
    </div>
</div>
</body>
</html>