# Feature ideas

These user-requested ideas are implemented in the Rider overview. More ideas can be added below as proposals.

## Usage history and small celebrations

### Weekly recap with a heatmap

The overview shows a compact Monday-to-Sunday heatmap of observed daily drops in remaining quota. It stores only
the starting and lowest reported percentages for each selected provider, locally, and includes a control to clear
the history. Provider reports can be incomplete, so the displayed changes are estimates rather than exact activity.

### Quota reset celebration

When a selected provider reports a new reset time and its remaining quota rises by at least 20 percentage points,
the overview briefly shows a "quota refilled" message. It does not celebrate the first snapshot or unavailable data.

## Playful usage guides

### Weekly quota boss fight

The collapsible weekly overview section turns observed quota drops into a cosmetic boss meter, capped at 100% for
the week. Each week rotates through six opponents with different colors and silhouettes. The Wraith changes phase
at 25%, 50%, 75%, and 100% observed weekly drop, with occasional one-line commentary. The short battle log includes
quota drops of at least five percentage points, resets, phase changes, and the week's arrival message. It has no
rewards or usage goals. Repeated hits from the same provider within 15 minutes are combined into one entry.

### Party lineup and reset glow

Selected providers appear as small party chips with their reported remaining quota. A reset briefly highlights the
matching chip and announces that its shield has recharged.

### Tactical forecast

Estimate when a selected provider may reach the 20% warning level from locally observed quota changes. Show an
estimate only after at least an hour and a three-point change, and omit it when a known reset arrives first. The
display labels the result as an estimate and explains why it may be unavailable.
