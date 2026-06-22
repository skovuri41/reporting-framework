# Reporting Framework Presentation - Flexible System

## 📁 What's Been Created

I've created a **flexible, editable presentation system** with the following files:

```
├── presentation-data.yaml       # 📊 Source of truth (25 KB)
├── generate_slides.py           # 🐍 Auto-generator script (6.2 KB)
├── PRESENTATION_GUIDE.md        # 📖 Usage guide (4 KB)
└── PRESENTATION_README.md       # 📋 This file
```

## 🎯 Key Benefits

✅ **Separate Content from Format** - Edit data without touching presentation logic
✅ **Version Control Friendly** - Track changes in YAML
✅ **Easy Edits** - Change numbers, text, add slides - just edit YAML
✅ **Regenerate Anytime** - One command to rebuild presentation
✅ **Multiple Outputs** - Can generate PowerPoint, PDF, HTML, Markdown

## 📊 Presentation Content (10 Slides)

All data is in `presentation-data.yaml`:

1. **Executive Summary** - Metrics comparison table
2. **Common Foundation** - What all 3 approaches share
3. **Option 1: POJO** - Code examples, metrics, pros/cons
4. **Option 2: Dataset-impl** - Dual-mode approach, custom API
5. **Option 3: Spark-SQL** - Local JVM, industry-standard API
6. **Development Effort** - Code comparison (55 vs 12 vs 7 lines)
7. **Transformation Capabilities** - Feature comparison table
8. **Resource Analysis** - JAR sizes, memory, startup times
9. **Decision Matrix** - Scenario-based recommendations
10. **Next Steps** - Summary and recommendations

## 🚀 Quick Start

### Option 1: Generate PowerPoint Slides (Automated)

```bash
# Install dependencies
pip install python-pptx pyyaml

# Generate slides
python generate_slides.py

# Output: presentation.pptx
```

### Option 2: Ask Claude to Generate

Just ask me:
```
"Generate PowerPoint slides from presentation-data.yaml"
```

I'll create properly formatted slides with:
- Title slides
- Bullet points
- Tables
- Code examples
- Diagrams

## ✏️ Making Edits

### Example 1: Update a Metric

**File:** `presentation-data.yaml`

Find the metric you want to change:
```yaml
rows:
  - ["JAR size", "3.6 MB", "3.8 MB", "20.7 MB"]
```

Change the value:
```yaml
rows:
  - ["JAR size", "3.6 MB", "3.8 MB", "25 MB"]  # Updated
```

### Example 2: Add a Bullet Point

```yaml
- type: bullets
  title: "Strengths"
  items:
    - "✅ Existing point 1"
    - "✅ Existing point 2"
    - "✅ New point you're adding"  # Add here
```

### Example 3: Update Code Example

```yaml
- type: code
  language: java
  content: |
    // Your updated code here
    DataSet result = service.executeDynamic("employees", params);
    // Add more lines...
```

### Example 4: Change Table Data

```yaml
- type: table
  headers: ["Metric", "POJO", "Dataset-impl", "Spark-SQL"]
  rows:
    - ["Code per report", "55 lines", "12 lines", "7 lines"]
    - ["New metric", "Value 1", "Value 2", "Value 3"]  # Add row
```

## 🔄 Edit → Regenerate Workflow

1. **Edit** `presentation-data.yaml`
   - Change metrics, update text, add slides
   - Keep YAML structure intact (2-space indentation)

2. **Validate** (optional)
   ```bash
   python -c "import yaml; yaml.safe_load(open('presentation-data.yaml'))"
   ```

3. **Regenerate**
   ```bash
   python generate_slides.py
   ```
   Or ask me: "Regenerate slides from YAML"

4. **Review** `presentation.pptx`

5. **Iterate** as needed

## 📝 Current Data Points (Based on Analysis)

The YAML includes these actual measurements:

### Code Metrics
- **POJO approach:** ~1,200 LOC, 14 files, 55 lines per report
- **Dataset-impl:** ~1,871 LOC, 20 files, 12 lines per report
- **Spark-SQL:** ~1,400 LOC (est.), 16 files (est.), 7 lines per report

### Dependencies
- **POJO:** 3.6 MB JAR (Jackson, SLF4J, JDBC)
- **Dataset-impl:** 3.8 MB JAR (same + custom classes)
- **Spark-SQL:** 20.7 MB JAR (+ spark-sql local library)

### Runtime
- **POJO:** 512 MB memory, <1s startup
- **Dataset-impl:** 512 MB memory, <1s startup
- **Spark-SQL:** 768 MB memory, 2-3s startup (SparkSession init)

### API Complexity
- **POJO:** Zero (native Java Streams)
- **Dataset-impl:** 56 custom methods across 4 classes
- **Spark-SQL:** Industry-standard Spark SQL API

### Code Reduction
- POJO (baseline): 55 lines
- Dataset-impl: 12 lines (**78% reduction**)
- Spark-SQL: 7 lines (**87% reduction**)

## 🎨 Customization Options

### Change Content
Edit `presentation-data.yaml` - all content is there

### Change Formatting
Edit `generate_slides.py`:
- Font sizes (currently: title 28pt, body 14pt, code 10pt)
- Colors (currently: default theme)
- Layouts (currently: title + content)
- Spacing

### Add New Slide Types
Add to `generate_slides.py`:
- Charts (requires python-pptx charts)
- Images (requires adding image paths)
- Custom layouts

## 🛠️ Advanced Options

### Generate Markdown Instead
```python
# Create yaml_to_markdown.py
import yaml

data = yaml.safe_load(open('presentation-data.yaml'))
for slide in data['slides']:
    print(f"## {slide['title']}\n")
    # ... format sections as markdown
```

### Generate HTML (Reveal.js)
```python
# Create yaml_to_reveal.py
# Convert to reveal.js HTML format
```

### Generate PDF
```bash
# After generating PowerPoint
# Use LibreOffice or PowerPoint to export to PDF
libreoffice --headless --convert-to pdf presentation.pptx
```

## 📚 Documentation

- **PRESENTATION_GUIDE.md** - Detailed usage guide
- **presentation-data.yaml** - All content (with inline comments)
- **generate_slides.py** - Generator script (with docstrings)

## 💡 Tips

1. **Keep backups** - Copy YAML before major edits
2. **Small changes first** - Test with minor edits
3. **Validate YAML** - Use Python to check syntax
4. **Review generated output** - Check slides after regeneration
5. **Version control** - Commit YAML to git

## 🔮 Future Enhancements

Possible improvements:
- [ ] Better table formatting in PowerPoint
- [ ] Syntax highlighting for code blocks
- [ ] Chart generation from data
- [ ] Multiple theme support
- [ ] Export to Google Slides format
- [ ] Interactive HTML version

## 🙋 Getting Help

**To regenerate slides:**
```
Ask me: "Generate slides from presentation-data.yaml"
```

**To validate YAML:**
```
python -c "import yaml; yaml.safe_load(open('presentation-data.yaml'))"
```

**To add new content:**
Edit `presentation-data.yaml` and follow the existing structure

---

**Ready to generate your presentation?**

1. Review `presentation-data.yaml` content
2. Make any edits you want
3. Run `python generate_slides.py` OR ask me to generate slides
4. Open `presentation.pptx` and review

Let me know if you'd like me to generate the slides now or if you want to edit the YAML first!
