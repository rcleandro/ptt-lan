import os
import re

root_dir = '/Users/leandro/StudioProjects/PTT'
pattern = re.compile(r'(implementation|api|testImplementation)\(project\("(:[^"]+)"\)\)')

for dirpath, _, filenames in os.walk(root_dir):
    if 'buildSrc' in dirpath or '.gradle' in dirpath:
        continue
    for filename in filenames:
        if filename.endswith('.gradle.kts'):
            filepath = os.path.join(dirpath, filename)
            with open(filepath, 'r') as f:
                content = f.read()
            
            new_content = pattern.sub(r'\1(project.dependencies.project("\2"))', content)
            
            if new_content != content:
                with open(filepath, 'w') as f:
                    f.write(new_content)
                print(f"Updated {filepath}")
