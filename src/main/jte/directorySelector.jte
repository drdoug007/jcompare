<div id="directory-modal" class="hidden fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
    <div class="relative top-20 mx-auto p-5 border w-1/2 shadow-lg rounded-md bg-white">
        <div class="mt-3">
            <h3 class="text-lg leading-6 font-medium text-gray-900" id="modal-title">Select Directory</h3>
            <div class="mt-2 px-7 py-3">
                <div class="flex items-center space-x-2 mb-4">
                    <button onclick="goUp()" class="p-2 bg-gray-200 rounded hover:bg-gray-300">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19V5"></path><path d="M5 10l7-7m0 0l7 7m-7-7v18"></path></svg>
                    </button>
                    <input type="text" id="current-path-display" class="flex-grow p-2 border rounded bg-gray-100" readonly>
                </div>
                <div id="directory-list" class="max-h-60 overflow-y-auto border rounded p-2">
                    <!-- Directory items will be injected here -->
                </div>
            </div>
            <div class="items-center px-4 py-3 flex justify-end space-x-3">
                <button id="cancel-btn" class="px-4 py-2 bg-gray-300 text-black rounded hover:bg-gray-400">Cancel</button>
                <button id="select-btn" class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">Select</button>
            </div>
        </div>
    </div>
</div>

<script>
    let currentPath = "";
    let targetInputId = "";

    function openDirectorySelector(inputId) {
        targetInputId = inputId;
        currentPath = document.getElementById(inputId).value || "";
        document.getElementById('directory-modal').classList.remove('hidden');
        loadDirectories(currentPath);
    }

    async function loadDirectories(path) {
        if (typeof showLoading === 'function') showLoading();
        try {
            const response = await fetch('/api/ls?path=' + encodeURIComponent(path));
            const dirs = await response.json();
            const listContainer = document.getElementById('directory-list');
            listContainer.innerHTML = '';
            
            // Update display path
            document.getElementById('current-path-display').value = dirs.length > 0 ? (path || "Home") : path;
            if (!path && dirs.length > 0) {
                // If path was empty, we are at home, let's find out what that path is from the first item's parent
                // But our API doesn't return parent. Let's assume the API handles empty as home.
            }

            dirs.forEach(dir => {
                const div = document.createElement('div');
                div.className = "flex items-center p-2 hover:bg-blue-50 cursor-pointer rounded";
                div.onclick = () => {
                    currentPath = dir.path;
                    loadDirectories(currentPath);
                };
                div.innerHTML = '<span class="mr-2">📁</span><span>' + dir.name + '</span>';
                listContainer.appendChild(div);
            });
        } finally {
            if (typeof hideLoading === 'function') hideLoading();
        }
    }

    function goUp() {
        if (!currentPath) return;
        const parts = currentPath.split('/');
        parts.pop();
        currentPath = parts.join('/') || "/";
        loadDirectories(currentPath);
    }

    document.getElementById('cancel-btn').onclick = () => {
        document.getElementById('directory-modal').classList.add('hidden');
    };

    document.getElementById('select-btn').onclick = () => {
        document.getElementById(targetInputId).value = currentPath;
        if (typeof onDirectorySelected === 'function') {
            onDirectorySelected(targetInputId, currentPath);
        }
        document.getElementById('directory-modal').classList.add('hidden');
    };
</script>
