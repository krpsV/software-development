#!/bin/bash
# プロジェクトルートディレクトリに移動
cd "$(dirname "$0")/.."

read -p "クライアントを起動しますか？ (y/n): " answer
if [[ $answer == "y" ]]; then
  mkdir -p bin
  javac -d bin src/*.java && java -cp bin JabberClient 9000; read
else
  echo "クライアントの起動をキャンセルしました。"
  read
fi 