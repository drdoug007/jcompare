<div id="diff-modal" class="hidden fixed inset-0 bg-gray-600 bg-opacity-75 overflow-y-auto h-full w-full z-50">
    <div class="relative top-2 mx-auto border w-[98vw] h-[96vh] shadow-2xl rounded-lg bg-white overflow-hidden flex flex-col">
        <div class="p-4 bg-blue-800 text-white flex justify-between items-center">
            <h3 class="text-xl font-bold">File Comparison</h3>
            <button onclick="closeDiffModal()" class="text-white hover:text-gray-300">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
            </button>
        </div>
        <div class="flex-grow">
            <iframe id="diff-iframe" class="w-full h-full border-none" src="about:blank"></iframe>
        </div>
        <div class="p-4 bg-gray-50 border-t flex justify-end">
            <button onclick="closeDiffModal()" class="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 font-bold transition-colors">Close</button>
        </div>
    </div>
</div>
