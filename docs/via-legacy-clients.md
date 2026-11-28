# Legacy clients on modern Paper/Folia servers

This compatibility path is for an AuthMe Paper/Folia build running on a modern
server, including 26.3, with ViaVersion, ViaBackwards and ViaRewind installed on
the **backend**. It does not require a legacy AuthMe server build or Java 8.
Via remains responsible for translating Minecraft packets.

## Authentication behavior

AuthMe checks the actual client protocol before opening a dialog. Java clients
older than 1.21.6 (including 1.8 protocol 47) use the normal protected command
authentication flow:

- Registered accounts must use `/login <password>`.
- New accounts use `/register` with the arguments required by the server's
  registration settings.
- Accounts with two-factor authentication must still use `/2fa code <code>`.
- Existing configured login/register reminders, timeouts, CAPTCHA, movement and
  interaction restrictions continue to apply.

These clients do not create a blocking pre-join dialog session. Skipping an
unsupported UI does not mark the account authenticated. Existing verified
premium, proxy and session authentication rules still apply.

Clients supporting dialogs keep the configured pre-join and in-game dialog
flows. Administrators do not need to disable dialogs globally to admit 1.8
players.

## Protocol detection

Before the player joins, Via's UUID connection index may not be populated.
AuthMe therefore matches Via's live server-side connections using Paper's raw
transport address, including the port. Forwarded addresses, names, and IP-only
matches cannot identify a connection safely.

After join, the same connection match takes precedence over Bukkit's translated
protocol, with Via's UUID API as a fallback. If Via is enabled but the client
protocol is unknown, AuthMe uses command authentication. Without Via, native
Paper protocol information is used. ViaVersion remains an optional dependency.

Via installed only on a proxy is outside this detection path: a backend cannot
infer the original client version from the proxy's translated connection alone.

## Validation before deployment

Unit tests cover protocol 47, the dialog protocol boundary, unknown protocols,
connections sharing an IP, early connections without a UUID index, protected
post-join fallback and mandatory two-factor authentication. These tests do not
replace a real 26.3 server connection test.

On a staging server with the actual Via plugins, connect with both 1.8 and a
current client. Check registration, wrong passwords, successful login, 2FA,
timeouts, disconnect/reconnect, and attempts to move, interact or access
inventory before authentication. The current client should retain dialogs;
1.8 should receive the configured command reminders without hanging in the
configuration phase.

References:

- [ViaVersion API usage](https://github.com/ViaVersion/ViaVersion/wiki/Basic-API-usage)
- [ViaVersion connection manager](https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/connection/ConnectionManager.java)
- [ViaRewind](https://github.com/ViaVersion/ViaRewind)
