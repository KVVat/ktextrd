#!/bin/bash

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


# Check for input argument
if [ -z "$1" ]; then
  usage
fi

# --- Prerequisite Checks ---
echo "Checking prerequisites..."
check_command "java" "Java is required, often for apksigner to function correctly."
check_command "apksigner" "apksigner is required to verify APK signatures. It's part of the Android SDK Build Tools."

SYSTEM_IMAGE="$1"
DIRECTORY_NAME="output"
[ -d "$DIRECTORY_NAME" ] || { echo "Creating directory '$DIRECTORY_NAME'..." && mkdir "$DIRECTORY_NAME"; }

./ktextrd.sh $SYSTEM_IMAGE -wx *.apk:output
find output -type f -name "*.apk" -print0 | while IFS= read -r -d $'\0' full_file_path; do
   #echo "Full file path: $full_file_path"
   filename="${full_file_path##*/}"
   echo "apkname : $filename"
   apksigner verify --print-certs $full_file_path
   #echo "$filename"
done


