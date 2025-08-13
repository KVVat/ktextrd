#!/bin/bash

# List all apk files under /priv-app/ in the multiple images in a directory
# The file name of images should be end with ext4 or img.
# It helps to find images which contains preloaded apk files in the system.
# The command take a directory name as an argument

# Function to display usage
usage() {
  echo "Usage: $0 <system_image_path>"
  exit 1
}
# Function to check if a command is available
check_command() {
  local cmd_name="$1"
  local cmd_purpose="$2" # Optional: for a more descriptive error message

  if ! command -v "$cmd_name" &> /dev/null; then
    echo "Error: Command '$cmd_name' not found."
    if [ -n "$cmd_purpose" ]; then
      echo "$cmd_purpose"
    fi
    echo "Please install it or ensure it's in your PATH."
    exit 1
  fi
  echo "Command '$cmd_name' is available." # Confirmation message
}

# --- Prerequisite Checks ---
echo "Checking prerequisites..."
check_command "java" "Java is required, often for apksigner to function correctly."

DIRECTORY_NAME="$1"

#echo "Extracting APKs (assuming ktextrd.sh handles this and places them in $DIRECTORY_NAME)..."
find $DIRECTORY_NAME -type f \( -name "*.ext4" -o -name "*.img" \) -print0 | while IFS= read -r -d $'\0' full_file_path; do
   echo "Full file path: $full_file_path"
   ./ktextrd.sh -l ${full_file_path} | grep /priv-app/.*.apk$
done


