#!/bin/bash

#exec 2>> packagecheck_errors.log

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
#if [ -z "$1" ]; then
#  usage
#fi

# --- Prerequisite Checks ---
echo "Checking prerequisites..."
check_command "java" "Java is required, often for apksigner to function correctly."
check_command "aapt2" "aapt2 is required to verify APK fiies. It's part of the Android SDK Build Tools."
check_command "apksigner" "apksigner is required to verify APK signatures."
check_command "apkanalyzer" "apkanalyzer is required to verify APK manifest."

SYSTEM_IMAGE="$1"
#IMAGE_WITHOUT_EXT=${SYSTEM_IMAGE%%.*}
#echo IMAGE_WITHOUT_EXT : $IMAGE_WITHOUT_EXT
DIRECTORY_NAME="../super"
[ -d "$DIRECTORY_NAME" ] || { echo "Creating directory '$DIRECTORY_NAME'..." && mkdir "$DIRECTORY_NAME"; }

#echo "Extracting APKs (assuming ktextrd.sh handles this and places them in $DIRECTORY_NAME)..."
#./ktextrd.sh $SYSTEM_IMAGE -wx *.apk:$DIRECTORY_NAME &> /dev/null
#./ktextrd.sh $SYSTEM_IMAGE -wx *.apk:$DIRECTORY_NAME
find $DIRECTORY_NAME -type f -name "*.ext4" -print0 | while IFS= read -r -d $'\0' full_file_path; do
   echo "Full file path: $full_file_path"
   ./ktextrd.sh -l ${full_file_path} | grep /priv-app/.*.apk$
   #filename="${full_file_path##*/}"
   #package_name=$(aapt2 dump packagename "$full_file_path")
   #if [[ "$full_file_path" == *priv-app* ]]; then
   #  echo "------------------------------"
   #  echo "apkname : $filename"
   #  echo "package_name : $package_name"
   #  aapt2 dump badging "$full_file_path" 2>/dev/null | grep "uses-permission"
   #fi
   #echo "$filename"
done


