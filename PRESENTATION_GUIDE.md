# Presentation Generation Guide

## Overview

The presentation data is stored in `presentation-data.yaml` in a structured format. This allows you to:
- ✅ Edit content without touching slide formatting
- ✅ Version control your presentation
- ✅ Regenerate slides anytime
- ✅ Generate multiple output formats (PowerPoint, PDF, HTML, Markdown)

## File Structure

```
presentation-data.yaml    # Source of truth for all content
PRESENTATION_GUIDE.md     # This file
presentation.pptx         # Generated PowerPoint (after running generator)
presentation.pdf          # Generated PDF (optional)
```

## Editing the Content

Simply edit `presentation-data.yaml`:

```yaml
slides:
  - slide_number: 1
    title: "Your Title"
    sections:
      - type: text
        content: "Your content here"

      - type: table
        headers: ["Col1", "Col2"]
        rows:
          - ["Data1", "Data2"]
```

### Supported Section Types

1. **text** - Plain text or markdown
2. **table** - Tabular data with headers and rows
3. **bullets** - Bulleted lists
4. **code** - Code snippets with syntax highlighting
5. **diagram** - ASCII diagrams or text-based diagrams
6. **callout** - Highlighted important information
7. **code_comparison** - Side-by-side code comparison

## Generating Slides

### Option 1: Using Python with python-pptx

```bash
# Install required package
pip install python-pptx pyyaml

# Run the generator script
python generate_slides.py
```

### Option 2: Using Marp (Markdown Presentations)

```bash
# Install Marp CLI
npm install -g @marp-team/marp-cli

# Convert YAML to Markdown first, then generate slides
python yaml_to_marp.py
marp presentation.md -o presentation.pptx
```

### Option 3: Using Reveal.js (HTML presentations)

```bash
# Convert YAML to reveal.js HTML
python yaml_to_reveal.py
# Open presentation.html in browser
```

### Option 4: Manual Generation (Current Approach)

Ask me (Claude) to generate slides from the YAML:
```
"Generate PowerPoint slides from presentation-data.yaml"
```

## Quick Edits - Common Scenarios

### Update a metric in a table

Find the table in `presentation-data.yaml`:
```yaml
rows:
  - ["JAR size", "3.6 MB", "3.8 MB", "20.7 MB"]  # Edit values here
```

### Add a new bullet point

```yaml
- type: bullets
  title: "Strengths"
  items:
    - "✅ Existing point"
    - "✅ New point you're adding"  # Add here
```

### Update code example

```yaml
- type: code
  language: java
  content: |
    // Your updated code here
    public class Example {
        // ...
    }
```

### Change slide order

Simply reorder the slides in the YAML file - they're numbered for easy reference.

## Regeneration Workflow

1. **Edit** `presentation-data.yaml`
2. **Validate** YAML syntax (optional):
   ```bash
   python -c "import yaml; yaml.safe_load(open('presentation-data.yaml'))"
   ```
3. **Generate** slides using one of the options above
4. **Review** generated output
5. **Iterate** if needed

## Tips

- Keep the YAML structure intact when editing
- Use proper indentation (2 spaces per level)
- Put multi-line content in `|` blocks
- Test with small changes first
- Keep backup of working version

## Example: Adding a New Slide

```yaml
slides:
  # ... existing slides ...

  - slide_number: 11  # New slide
    title: "Additional Considerations"
    sections:
      - type: bullets
        title: "Security"
        items:
          - "SQL injection prevention"
          - "Connection string encryption"

      - type: table
        headers: ["Feature", "Status"]
        rows:
          - ["Audit logging", "✅ Implemented"]
          - ["Role-based access", "⚠️ Planned"]
```

## Next Steps

1. **Review** the current YAML content
2. **Make edits** as needed
3. **Request regeneration** or use automated tools
4. **Iterate** based on feedback

---

**Need help?** Ask me to:
- Generate PowerPoint slides from the YAML
- Convert YAML to Markdown format
- Create a Python script to automate generation
- Validate your YAML changes
