<!DOCTYPE html>
<html lang="ta">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>மின்சார துண்டிப்பு அறிவிப்பு</title>
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
        <h2>மின்சார துண்டிப்பு அறிவிப்பு</h2>
    </div>

    <div class="content">
        <p>வணக்கம் ${username},</p>

        <p>உங்கள் பகுதியில் திட்டமிடப்பட்ட <strong>${outageType}</strong> மின்சார துண்டிப்பு பற்றி உங்களுக்குத் தெரிவிக்க இது.</p>

        <div class="alert-info">
            <p>இந்த மின்துண்டிப்பு <strong>${areaName}</strong> மற்றும் சுற்றுப்புற பகுதிகளை பாதிக்கும்.</p>
        </div>

        <div class="outage-details">
            <h3>மின்துண்டிப்பு விவரங்கள்:</h3>
            <table>
                <tr>
                    <td>நிலை:</td>
                    <td>${status}</td>
                </tr>
                <tr>
                    <td>தொடக்க நேரம்:</td>
                    <td>${startTime}</td>
                </tr>
                <#if endTime??>
                    <tr>
                        <td>மதிப்பிடப்பட்ட முடிவு நேரம்:</td>
                        <td>${endTime}</td>
                    </tr>
                </#if>
                <#if reason??>
                    <tr>
                        <td>காரணம்:</td>
                        <td>${reason}</td>
                    </tr>
                </#if>
                <#if additionalInfo??>
                    <tr>
                        <td>கூடுதல் தகவல்:</td>
                        <td>${additionalInfo}</td>
                    </tr>
                </#if>
            </table>
        </div>

        <#if portalUrl??>
            <p>
                <a href="${portalUrl}" class="button">மின்துண்டிப்பு விவரங்களைக் காண</a>
            </p>
        </#if>

        <div class="tips">
            <h3>மின்துண்டிப்பின் போது:</h3>
            <ul>
                <li>குளிர்சாதனப் பெட்டி மற்றும் உறைவிப்பான் கதவுகளை மூடி வைக்கவும்</li>
                <li>உணர்திறன் மின்னணு சாதனங்களை அணைத்து, மின்சாரத்தில் இருந்து துண்டிக்கவும்</li>
                <li>அவசர விளக்குகள் அல்லது மெழுகுவர்த்திகளை தயாராக வைக்கவும்</li>
                <li>வயதானவர்கள் அல்லது மருத்துவ தேவைகள் உள்ளவர்களை சரிபார்க்கவும்</li>
            </ul>
        </div>

        <p>இதனால் ஏற்படும் எந்த சிரமத்திற்கும் நாங்கள் வருந்துகிறோம், உங்கள் பொறுமைக்கு நன்றி.</p>

        <p>நன்றி,<br>Power Alert குழு</p>
    </div>

    <div class="footer">
        <p>இது ஒரு தானியங்கி அறிவிப்பு. இந்த மின்னஞ்சலுக்கு பதிலளிக்க வேண்டாம்.</p>
        <p>&copy; 2025 Power Alert | <a href="https://poweralert.lk">poweralert.lk</a></p>
    </div>
</div>
</body>
</html>