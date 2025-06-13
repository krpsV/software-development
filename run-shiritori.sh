#!/bin/bash

# しりとりゲーム実行スクリプト
echo "しりとりゲーム用tmuxセッション開始"

# Javaファイルをコンパイル
echo "Javaファイルをコンパイル中..."
javac -d bin src/bootcamp/*.java

if [ $? -ne 0 ]; then
    echo "コンパイルエラー"
    exit 1
fi

# 既存のセッションがあれば終了
tmux kill-session -t shiritori 2>/dev/null || true

# tmuxセッション作成
tmux new-session -d -s shiritori

# 左側でサーバー起動
tmux send-keys -t shiritori 'echo "サーバーを起動中..."' Enter
tmux send-keys -t shiritori 'java -cp bin bootcamp.ShiritoriServer' Enter

# 右側を分割してクライアント用
tmux split-window -h -t shiritori

# 右側をさらに分割（複数クライアント用）
tmux split-window -v -t shiritori

# クライアント起動の説明
tmux send-keys -t shiritori.1 'echo "クライアント1: java -cp bin bootcamp.ShiritoriClient でゲームに参加"' Enter
tmux send-keys -t shiritori.2 'echo "クライアント2: java -cp bin bootcamp.ShiritoriClient でゲームに参加"' Enter

# セッションにアタッチ
tmux attach-session -t shiritori