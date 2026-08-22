---
layout: page
title: NocturneL User Manual
description: Complete instructions for setting up and using the NocturneL Android music player.
permalink: /manual/
---

# NocturneL user manual

This manual covers NocturneL on Android 12+ from the first folder scan through playback, playlists, visualizers, settings, and troubleshooting. Choose a topic below or read from the beginning.

{% assign manual_pages = site.manual | sort: "nav_order" %}
<ol class="topic-grid">
{% for entry in manual_pages %}
  <li>
    <a href="{{ entry.url | relative_url }}">
      <strong>{{ entry.title }}</strong>
      <span>{{ entry.description }}</span>
    </a>
  </li>
{% endfor %}
</ol>

For questions not answered here, email [{{ site.support_email }}](mailto:{{ site.support_email }}). For details about local data, see the [Privacy Policy]({{ '/privacy/' | relative_url }}).
