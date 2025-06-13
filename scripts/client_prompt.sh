#!/bin/bash
# プロジェクトルートディレクトリに移動
cd "$(dirname "$0")/.."

# 環境変数PORTが設定されていない場合は9000をデフォルトとして使用
PORT=${PORT:-9000}

read -p "クライアントを起動しますか？ (port: $PORT) (y/n): " answer
if [[ $answer == "y" ]]; then
  mkdir -p bin
  javac -d bin src/bootcamp/*.java && java -cp bin JabberClient $PORT; read
else
  echo "クライアントの起動をキャンセルしました。"
  read
fi 