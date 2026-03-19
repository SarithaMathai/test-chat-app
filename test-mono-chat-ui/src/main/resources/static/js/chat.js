const API_BASE = 'http://localhost:8080';
const WS_ENDPOINT = API_BASE + '/ws';

let stompClient = null;
let connected = false;

function connect() {
    const username = document.getElementById('username').value.trim();
    if (!username) {
        alert('Please enter your name before connecting.');
        return;
    }

    const socket = new SockJS(WS_ENDPOINT);
    stompClient = Stomp.over(socket);
    stompClient.debug = function (message) {
        console.log('[STOMP]', message);
    };

    stompClient.connect({}, function () {
        console.log('[CHAT] Connected to WebSocket endpoint:', WS_ENDPOINT);
        connected = true;
        document.getElementById('message-input').disabled = false;
        document.getElementById('send-btn').disabled = false;
        document.getElementById('connect-btn').disabled = true;
        document.getElementById('username').disabled = true;

        stompClient.subscribe('/topic/messages', function (message) {
            const chatMessage = JSON.parse(message.body);
            console.log('[CHAT] Received message from /topic/messages:', chatMessage);
            appendMessage(chatMessage);
        });

        loadHistory();
    }, function (error) {
        console.error('WebSocket connection error:', error);
        alert('Failed to connect to chat server. Please ensure the API is running on port 8080.');
    });
}

function sendMessage() {
    if (!connected || !stompClient) return;

    const username = document.getElementById('username').value.trim();
    const input = document.getElementById('message-input');
    const content = input.value.trim();

    if (!content) return;

    console.log('[CHAT] Sending message to /app/chat.send:', {
        sender: username,
        content: content
    });
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        sender: username,
        content: content
    }));

    input.value = '';
}

function loadHistory() {
    console.log('[CHAT] Loading history from:', API_BASE + '/api/chat/messages');
    fetch(API_BASE + '/api/chat/messages')
        .then(function (response) { return response.json(); })
        .then(function (messages) {
            console.log('[CHAT] Loaded history:', messages);
            const container = document.getElementById('messages');
            container.innerHTML = '';
            messages.forEach(function (msg) { appendMessage(msg); });
        })
        .catch(function (err) {
            console.warn('Could not load message history:', err);
        });
}

function appendMessage(message) {
    const messagesDiv = document.getElementById('messages');
    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';

    const timestamp = message.timestamp
        ? new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : '';

    bubble.innerHTML =
        '<div class="sender">' + escapeHtml(message.sender) + '</div>' +
        '<div class="content">' + escapeHtml(message.content) + '</div>' +
        '<div class="timestamp">' + timestamp + '</div>';

    messagesDiv.appendChild(bubble);
    messagesDiv.parentElement.scrollTop = messagesDiv.parentElement.scrollHeight;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('message-input').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') sendMessage();
    });
});
