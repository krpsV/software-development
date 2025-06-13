#!/bin/bash

SESSION="jabber"

# 既存のセッションがあればkill
tmux kill-session -t $SESSION 2>/dev/null

# binディレクトリを作成
mkdir -p bin

# 新しいセッションを作成し、サーバーを左ペインで起動
tmux new-session -d -s $SESSION "javac -d bin src/*.java && java -cp bin JabberServer 9000; read"

# 右側にペインを分割し、クライアント起動確認スクリプトを実行
tmux split-window -h -t $SESSION "bash scripts/client_prompt.sh"

# ペインのサイズを調整（任意）
tmux select-layout -t $SESSION even-horizontal

# tmuxセッションにアタッチ
tmux attach -t $SESSION