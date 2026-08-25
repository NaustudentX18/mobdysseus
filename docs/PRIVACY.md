# Mobdysseus — Privacy

Mobdysseus is built to be private by default. This page explains, in plain
language, what the app does and doesn't do with your data.

## The short version

- **No account.** You never sign up or sign in. There is nothing to create,
  and nothing we can tie back to you.
- **No telemetry.** The app does not phone home, and it does not send usage
  or crash reports to us. We never see how you use the app.
- **No ads.** There are no ad SDKs and no tracking SDKs in the app.
- **No selling data.** We do not sell, rent, share, or trade your data. We
  don't even have a copy of it to sell.

## Where your data lives

By default, everything stays **on your device**, in the app's private storage
(which Android keeps separate from other apps):

- Chat history
- Notes
- Tasks
- Documents
- Calendar entries
- Memory / saved context
- On-device model files you download

None of this leaves your phone unless you explicitly ask it to. Uninstalling
the app removes this local data.

## When the app does use the network

The app only contacts the network in three situations, and only after you
deliberately configure them:

1. **Cloud chat.** If you add an API provider (for example OpenAI, DeepSeek,
   Ollama, or a custom endpoint) for cloud chat, your chat messages are sent
   to that provider so it can generate replies. That provider's own privacy
   policy then applies to the messages you send it. We never see them.
2. **MCP server.** If you configure an MCP (Model Context Protocol) server,
   the app talks to the server you specified. Whatever you configure and
   whatever that server does with your requests is between you and that server.
3. **Downloading an on-device model.** If you choose to download a model from
   Hugging Face for fully offline use, the app downloads that file once. The
   download itself is from Hugging Face's servers.

If you configure none of these, the app works entirely offline and never
contacts the network.

## AI-generated content

Mobdysseus generates text with AI models. Those outputs can be wrong,
outdated, misleading, or incomplete. Don't rely on them for medical, legal,
financial, or other important decisions without checking. You are responsible
for how you use what the app produces.

## Changes

If this policy ever changes, the updated version will ship with the app and
will be reflected in the source repository. There is no separate channel for
notifications because the app has no account and no way to reach you.
