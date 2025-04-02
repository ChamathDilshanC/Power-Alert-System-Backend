<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PowerAlert Login Notification</title>
    <style>
        :root {
            --primary-color: #3D5AFE;
            --primary-gradient: linear-gradient(135deg, #3D5AFE 0%, #1A75FF 100%);
            --accent-color: #00E5FF;
            --text-dark: #18191F;
            --text-body: #4E5D78;
            --text-light: #8A94A6;
            --bg-light: #F7F9FC;
            --bg-dark: #18191F;
            --bg-card: #FFFFFF;
            --border-color: #E9EDF5;
            --success-color: #38CB89;
            --warning-color: #FFAB00;
            --error-color: #FF5574;
            --shadow-sm: 0 2px 8px rgba(0,0,0,0.06);
            --shadow-md: 0 4px 16px rgba(0,0,0,0.08);
            --shadow-lg: 0 8px 30px rgba(0,0,0,0.12);
            --radius-sm: 8px;
            --radius-md: 12px;
            --radius-lg: 20px;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
            line-height: 1.6;
            color: var(--text-body);
            margin: 0;
            padding: 0;
            background-color: #F0F2F5;
        }

        .container {
            max-width: 600px;
            margin: 24px auto;
            background-color: var(--bg-card);
            border-radius: var(--radius-md);
            overflow: hidden;
            box-shadow: var(--shadow-md);
        }

        .header-banner {
            height: 6px;
            background: var(--primary-gradient);
        }

        .logo-section {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 24px 32px;
            border-bottom: 1px solid var(--border-color);
        }

        .logo {
            display: flex;
            align-items: center;
        }

        .logo img {
            width: 36px;
            height: 36px;
            border-radius: var(--radius-sm);
            object-fit: cover;
        }

        .logo-text {
            margin-left: 12px;
            font-weight: 600;
            font-size: 18px;
            color: var(--text-dark);
        }

        .timestamp {
            font-size: 14px;
            color: var(--text-light);
        }

        .main-content {
            padding: 32px;
        }

        .greeting {
            font-size: 24px;
            font-weight: 700;
            color: var(--text-dark);
            margin-bottom: 8px;
        }

        .subheading {
            font-size: 16px;
            font-weight: 500;
            color: var(--text-body);
            margin-bottom: 32px;
        }

        .alert-container {
            display: flex;
            align-items: flex-start;
            background-color: rgba(255, 171, 0, 0.08);
            border-left: 4px solid var(--warning-color);
            padding: 20px;
            border-radius: var(--radius-sm);
            margin-bottom: 32px;
        }

        .alert-icon {
            width: 24px;
            height: 24px;
            margin-right: 16px;
            color: var(--warning-color);
            flex-shrink: 0;
        }

        .alert-text {
            color: var(--text-dark);
            font-size: 15px;
            line-height: 1.6;
        }

        .info-card {
            background-color: var(--bg-light);
            border-radius: var(--radius-md);
            padding: 24px;
            margin-bottom: 32px;
        }

        .card-title {
            font-weight: 600;
            font-size: 16px;
            color: var(--text-dark);
            margin-bottom: 16px;
            display: flex;
            align-items: center;
        }

        .card-title-icon {
            width: 20px;
            height: 20px;
            margin-right: 8px;
        }

        .info-item {
            display: flex;
            margin-bottom: 16px;
            font-size: 15px;
        }

        .info-item:last-child {
            margin-bottom: 0;
        }

        .info-label {
            flex: 0 0 120px;
            font-weight: 500;
            color: var(--text-light);
        }

        .info-value {
            flex: 1;
            color: var(--text-dark);
            font-weight: 500;
        }

        .device-icon {
            width: 16px;
            height: 16px;
            margin-right: 8px;
            vertical-align: text-bottom;
        }

        .location-icon {
            width: 16px;
            height: 16px;
            margin-right: 8px;
            vertical-align: text-bottom;
        }

        .paragraph {
            font-size: 15px;
            line-height: 1.6;
            margin-bottom: 32px;
            color: var(--text-body);
        }

        .actions {
            display: flex;
            gap: 16px;
            margin-bottom: 32px;
        }

        .button-primary {
            display: inline-block;
            background: var(--primary-gradient);
            color: white;
            text-decoration: none;
            padding: 12px 24px;
            border-radius: var(--radius-sm);
            font-size: 15px;
            font-weight: 600;
            text-align: center;
            transition: all 0.2s ease;
            box-shadow: 0 4px 12px rgba(61, 90, 254, 0.2);
        }

        .button-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(61, 90, 254, 0.3);
        }

        .button-secondary {
            display: inline-block;
            background-color: var(--bg-light);
            color: var(--text-dark);
            text-decoration: none;
            padding: 12px 24px;
            border-radius: var(--radius-sm);
            font-size: 15px;
            font-weight: 600;
            text-align: center;
            transition: all 0.2s ease;
            border: 1px solid var(--border-color);
        }

        .button-secondary:hover {
            background-color: #EBEEF2;
            transform: translateY(-2px);
        }

        .notice {
            display: flex;
            align-items: flex-start;
            background-color: rgba(56, 203, 137, 0.08);
            border-left: 4px solid var(--success-color);
            padding: 16px;
            border-radius: var(--radius-sm);
            margin-bottom: 32px;
        }

        .notice-icon {
            width: 20px;
            height: 20px;
            margin-right: 12px;
            color: var(--success-color);
            flex-shrink: 0;
        }

        .notice-text {
            color: var(--text-dark);
            font-size: 14px;
            line-height: 1.5;
        }

        .footer {
            background-color: var(--bg-light);
            padding: 24px 32px;
            border-top: 1px solid var(--border-color);
        }

        .footer-content {
            text-align: center;
            font-size: 13px;
            color: var(--text-light);
            line-height: 1.6;
        }

        .footer-logo {
            margin-bottom: 16px;
        }

        .footer-logo img {
            height: 24px;
            width: auto;
        }

        .footer-links {
            margin: 16px 0;
        }

        .footer-links a {
            color: var(--primary-color);
            text-decoration: none;
            margin: 0 12px;
            font-weight: 500;
        }

        .footer-links a:hover {
            text-decoration: underline;
        }

        .social-links {
            display: flex;
            justify-content: center;
            gap: 16px;
            margin: 16px 0;
        }

        .social-icon {
            width: 20px;
            height: 20px;
        }

        @media (max-width: 640px) {
            .container {
                margin: 0;
                border-radius: 0;
            }

            .main-content {
                padding: 24px;
            }

            .logo-section {
                padding: 16px 24px;
            }

            .actions {
                flex-direction: column;
            }

            .button-primary, .button-secondary {
                width: 100%;
            }
            .space{
                width: 10px;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header-banner"></div>

    <div class="logo-section">
        <div class="logo">
            <img src="https://i.pinimg.com/736x/60/10/1e/60101ee7d062f3c2d3d60919e0e5cc06.jpg" alt="PowerAlert Logo">
            <span class="logo-text">PowerAlert</span>
        </div>
        <div class="timestamp">${loginTime}</div>
    </div>

    <div class="main-content">
        <h1 class="greeting">Hi ${username},</h1>
        <p class="subheading">We detected a new sign-in to your PowerAlert account</p>

        <div class="alert-container">
            <svg class="alert-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <div class="alert-text">
                For your security, we always notify you when your account is accessed from a new device or location.
            </div>
        </div>

        <div class="info-card">
            <div class="card-title">
                <svg class="card-title-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                    <line x1="16" y1="2" x2="16" y2="6"></line>
                    <line x1="8" y1="2" x2="8" y2="6"></line>
                    <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
                Login Details
            </div>

            <div class="info-item">
                <div class="info-label">Account :-</div><div class="space"></div>
                <div class="info-value">${username}</div>
            </div>

            <div class="info-item">
                <div class="info-label">Date & Time :-</div>
                <div class="info-value">${loginTime}</div>
            </div>

            <div class="info-item">
                <div class="info-label">Device :-</div><div class="space"></div>
                <div class="info-value">
                    <svg class="device-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                        <line x1="12" y1="18" x2="12.01" y2="18"></line>
                    </svg>
                    ${device}
                </div>
            </div>

            <#if location??>
                <div class="info-item">
                    <div class="info-label">Location :-</div><div class="space"></div>
                    <div class="info-value">
                        <svg class="location-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                            <circle cx="12" cy="10" r="3"></circle>
                        </svg>

                        ${location}
                    </div>
                </div>
            </#if>
        </div>

        <p class="paragraph">If this was you, you can safely ignore this email. If you don't recognize this activity, we recommend taking immediate action to secure your account.</p>

        <div class="actions">
            <a href="${accountSecurityUrl}" class="button-primary">Secure My Account</a>
            <a href="mailto:support@poweralert.lk" class="button-secondary">Contact Support</a>
        </div>

        <div class="notice">
            <svg class="notice-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            <div class="notice-text">
                We recommend using strong, unique passwords and enabling two-factor authentication to keep your account secure.
            </div>
        </div>
    </div>

    <div class="footer">
        <div class="footer-content">
            <div class="footer-logo">
                <img src="https://i.pinimg.com/736x/60/10/1e/60101ee7d062f3c2d3d60919e0e5cc06.jpg" alt="PowerAlert">
            </div>

            <p>This email was sent to ${email!'your account'} to notify you about important account activity.</p>

            <div class="footer-links">
                <a href="https://poweralert.lk/help">Help Center</a>
                <a href="https://poweralert.lk/terms">Terms</a>
                <a href="https://poweralert.lk/privacy">Privacy</a>
            </div>

            <div class="social-links">
                <a href="https://facebook.com/poweralert">
                    <svg class="social-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"></path>
                    </svg>
                </a>
                <a href="https://twitter.com/poweralert">
                    <svg class="social-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path>
                    </svg>
                </a>
                <a href="https://instagram.com/poweralert">
                    <svg class="social-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect>
                        <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                        <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line>
                    </svg>
                </a>
            </div>

            <p>© ${year} PowerAlert, Colombo, Sri Lanka</p>
            <p>If you don't want to receive emails about account activity, please <a href="${unsubscribeUrl}" style="color: var(--primary-color); text-decoration: none;">unsubscribe</a>.</p>
        </div>
    </div>
</div>
</body>
</html>