<!DOCTYPE html>
<html lang="si">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>විදුලි බිඳවැටීම් දැනුම්දීම</title>
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
        <h2>විදුලි බිඳවැටීම් දැනුම්දීම</h2>
    </div>

    <div class="content">
        <p>ආයුබෝවන් ${username},</p>

        <p>ඔබගේ ප්‍රදේශයේ සැලසුම් කළ <strong>${outageType}</strong> බිඳවැටීමක් පිළිබඳව ඔබව දැනුවත් කිරීමට මෙය වේ.</p>

        <div class="alert-info">
            <p>මෙම බිඳවැටීම <strong>${areaName}</strong> සහ අවට ප්‍රදේශවලට බලපානු ඇත.</p>
        </div>

        <div class="outage-details">
            <h3>බිඳවැටීම් විස්තර:</h3>
            <table>
                <tr>
                    <td>තත්ත්වය:</td>
                    <td>${status}</td>
                </tr>
                <tr>
                    <td>ආරම්භක වේලාව:</td>
                    <td>${startTime}</td>
                </tr>
                <#if endTime??>
                    <tr>
                        <td>අවසන් වීමට නියමිත වේලාව:</td>
                        <td>${endTime}</td>
                    </tr>
                </#if>
                <#if reason??>
                    <tr>
                        <td>හේතුව:</td>
                        <td>${reason}</td>
                    </tr>
                </#if>
                <#if additionalInfo??>
                    <tr>
                        <td>අතිරේක තොරතුරු:</td>
                        <td>${additionalInfo}</td>
                    </tr>
                </#if>
            </table>
        </div>

        <#if portalUrl??>
            <p>
                <a href="${portalUrl}" class="button">බිඳවැටීම් විස්තර බලන්න</a>
            </p>
        </#if>

        <div class="tips">
            <h3>විදුලි බිඳවැටීම් අතරතුර:</h3>
            <ul>
                <li>ශීතකරණ සහ අධිශීතකරණ දොරවල් වසා තබන්න</li>
                <li>සංවේදී ඉලෙක්ට්‍රොනික උපකරණ ක්‍රියා විරහිත කර විදුලි බස්නාවෙන් ගලවන්න</li>
                <li>හදිසි ආලෝකය හෝ ඉටිපන්දම් සූදානම් කර තබා ගන්න</li>
                <li>වැඩිහිටි අසල්වැසියන් හෝ වෛද්‍ය අවශ්‍යතා ඇති අය ගැන සොයා බලන්න</li>
            </ul>
        </div>

        <p>මෙම තත්ත්වය නිසා ඇතිවිය හැකි අපහසුතා පිළිබඳව අපි කනගාටු වන අතර ඔබගේ ඉවසීම අගය කරමු.</p>

        <p>ස්තූතියි,<br>Power Alert කණ්ඩායම</p>
    </div>

    <div class="footer">
        <p>මෙය ස්වයංක්‍රීය දැනුම්දීමකි. කරුණාකර මෙම විද්‍යුත් තැපෑලට පිළිතුරු නොදෙන්න.</p>
        <p>&copy; 2025 Power Alert | <a href="https://poweralert.lk">poweralert.lk</a></p>
    </div>
</div>
</body>
</html>